package io.kagera.akka.actor

import akka.actor.{ ActorLogging, ActorRef, PoisonPill, Props }
import akka.pattern.pipe
import akka.persistence.PersistentActor
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.kagera.akka.actor.PetriNetInstance.Settings
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.execution.EventSourcing._
import io.kagera.execution._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.existentials

object PetriNetInstance {

  case class Settings(evaluationStrategy: ExecutionContext, idleTTL: Option[FiniteDuration])

  val defaultSettings: Settings =
    Settings(evaluationStrategy = ExecutionContext.Implicits.global, idleTTL = Some(5 minutes))

  private case class IdleStop(seq: Long)

  def petriNetInstancePersistenceId(processId: String): String = s"process-$processId"

  def instanceState[S, T <: Transition[_, _, S]](instance: Instance[S, T]): InstanceState[S] = {
    val failures = instance.failedJobs.map { e =>
      e.transitionId -> PetriNetInstanceProtocol.ExceptionState(
        e.consecutiveFailureCount,
        e.failureReason,
        e.failureStrategy
      )
    }.toMap

    InstanceState[S](instance.sequenceNr, instance.marking, instance.state, failures)
  }

  def props[S, T <: Transition[Any, _, S]](topology: ExecutablePetriNet[S, T], settings: Settings = defaultSettings)(
    implicit executorFactory: TransitionExecutorFactory.WithInputOutputState[IO, T, Any, _, S]
  ): Props =
    Props(new PetriNetInstance[S, T](topology, settings, new TransitionExecutorImpl[IO, T](topology)))
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetInstance[S, T <: Transition[Any, _, S]](
  override val topology: ExecutablePetriNet[S, T],
  val settings: Settings,
  executor: TransitionExecutor[IO, T]
)(implicit executorFactory: TransitionExecutorFactory.WithInputOutputState[IO, T, Any, _, S])
    extends PersistentActor
    with ActorLogging
    with PetriNetInstanceRecovery[S, T] {

  import PetriNetInstance._

  val processId = context.self.path.name

  override def persistenceId: String = petriNetInstancePersistenceId(processId)

  import context.dispatcher

  override def receiveCommand = uninitialized

  def uninitialized: Receive = {
    case msg @ Initialize(marking, state) =>
      log.debug(s"Received message: {}", msg)
      val uninitialized = Instance.uninitialized[S, T](topology)
      persistEvent(uninitialized, InitializedEvent(marking, state.asInstanceOf[S])) {
        (applyEvent(uninitialized) _)
          .andThen(step)
          .andThen { _ => sender() ! Initialized(marking, state) }
      }
    case msg: Command =>
      sender() ! IllegalCommand("Only accepting Initialize commands in 'uninitialized' state")
      context.stop(context.self)
  }

  def running(instance: Instance[S, T]): Receive = {
    case IdleStop(n) if n == instance.sequenceNr && instance.activeJobs.isEmpty =>
      context.stop(context.self)

    case GetState =>
      log.debug(s"Received message: GetState")
      sender() ! instanceState(instance)

    case e @ TransitionFiredEvent(jobId, transitionId, timeStarted, timeCompleted, consumed, produced, output) =>
      log.debug(s"Received message: {}", e)
      log.debug(s"Transition '${topology.transitions.getById(transitionId)}' successfully fired")

      persistEvent(instance, e)(
        (applyEvent(instance) _)
          .andThen(step)
          .andThen { updatedInstance =>
            sender() ! TransitionFired[S](transitionId, e.consumed, e.produced, instanceState(updatedInstance))
          }
      )

    case e @ TransitionFailedEvent(jobId, transitionId, timeStarted, timeFailed, consume, input, reason, strategy) =>
      log.debug(s"Received message: {}", e)
      log.warning(s"Transition '${topology.transitions.getById(transitionId)}' failed with: {}", reason)

      val updatedInstance = applyEvent(instance)(e)

      def updateAndRespond(instance: Instance[S, T]) = {
        sender() ! TransitionFailed(transitionId, consume, input, reason, strategy)
        context become running(instance)
      }

      strategy match {
        case RetryWithDelay(delay) =>
          log.warning(
            s"Scheduling a retry of transition '${topology.transitions.getById(transitionId)}' in $delay milliseconds"
          )
          val originalSender = sender()
          system.scheduler.scheduleOnce(delay milliseconds) {
            executeJob(updatedInstance.jobs(jobId), originalSender)
          }
          updateAndRespond(applyEvent(instance)(e))
        case _ =>
          persistEvent(instance, e)((applyEvent(instance) _).andThen(updateAndRespond _))
      }

    case msg @ FireTransition(id, input, correlationId) =>
      log.debug(s"Received message: {}", msg)

      fireTransitionById[S, T](id, input).run(instance).value match {
        case (updatedInstance, Right(job)) =>
          executeJob(job, sender())
          context become running(updatedInstance)
        case (_, Left(reason)) =>
          log.warning(reason)
          sender() ! TransitionNotEnabled(id, reason)
      }
    case msg: Initialize[_] =>
      sender() ! IllegalCommand("Already initialized")
  }

  // TODO remove side effecting here
  def step(instance: Instance[S, T]): Instance[S, T] = {
    fireAllEnabledTransitions.run(instance).value match {
      case (updatedInstance, jobs) =>
        if (jobs.isEmpty && updatedInstance.activeJobs.isEmpty)
          settings.idleTTL.foreach { ttl =>
            log.debug("Process has no running jobs, killing the actor in: {}", ttl)
            system.scheduler.scheduleOnce(ttl, context.self, PoisonPill)
          }

        jobs.foreach(job => executeJob(job, sender()))
        context become running(updatedInstance)
        updatedInstance
    }
  }

  def executeJob(job: Job[S, T], originalSender: ActorRef) =
    runJobAsync[S, T](job, executor)(settings.evaluationStrategy, executorFactory)
      .unsafeToFuture()
      .pipeTo(context.self)(originalSender)

  override def onRecoveryCompleted(instance: Instance[S, T]) = step(instance)
}
