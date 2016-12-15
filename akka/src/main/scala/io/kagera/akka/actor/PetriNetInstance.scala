package io.kagera.akka.actor

import akka.actor.{ ActorLogging, ActorRef, Props }
import akka.pattern.pipe
import akka.persistence.PersistentActor
import fs2.Strategy
import io.kagera.akka.actor.PetriNetInstance.Settings
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored._
import io.kagera.execution.EventSourcing._
import io.kagera.execution._

import scala.concurrent.duration._
import scala.language.existentials

object PetriNetInstance {

  // private control message to stop the actor after a certain idle time.
  private case class IdleStop(markingSeq: Long)

  case class Settings(evaluationStrategy: Strategy, idleTTL: Option[FiniteDuration])

  val defaultSettings: Settings =
    Settings(evaluationStrategy = Strategy.fromCachedDaemonPool("Kagera.CachedThreadPool"), idleTTL = None)

  def petriNetInstancePersistenceId(processId: String): String = s"process-$processId"

  def instanceState[S](instance: Instance[S]): InstanceState[S] = {
    val failures = instance.failedJobs.map { e =>
      e.transitionId -> PetriNetInstanceProtocol.ExceptionState(
        e.consecutiveFailureCount,
        e.failureReason,
        e.failureStrategy
      )
    }.toMap

    InstanceState[S](instance.sequenceNr, instance.marking, instance.state, failures)
  }

  def props[S](topology: ExecutablePetriNet[S]): Props =
    Props(new PetriNetInstance[S](topology, defaultSettings, new TransitionExecutorImpl[S](topology)))
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetInstance[S](
  override val topology: ExecutablePetriNet[S],
  val settings: Settings,
  executor: TransitionExecutor[S]
) extends PersistentActor
    with ActorLogging
    with PetriNetInstanceRecovery[S] {

  import PetriNetInstance._

  val processId = context.self.path.name

  override def persistenceId: String = petriNetInstancePersistenceId(processId)

  import context.dispatcher

  override def receiveCommand = uninitialized

  def uninitialized: Receive = {
    case msg @ Initialize(marking, state) =>
      log.debug(s"Received message: {}", msg)
      persistEvent(Instance.uninitialized(topology), InitializedEvent(marking, state.asInstanceOf[S])) {
        (updatedState, e) =>
          step(updatedState)
          sender() ! Initialized(marking, state)
      }
    case msg: Command =>
      sender() ! IllegalCommand("Only accepting Initialize commands in 'uninitialized' state")
      context.stop(context.self)
  }

  def running(instance: Instance[S]): Receive = {
    case IdleStop(n) if n == instance.sequenceNr && instance.activeJobs.isEmpty =>
      log.info(s"Process was idle for '${settings.idleTTL}, stopping the actor")
      context.stop(context.self)
    case GetState =>
      log.debug(s"Received message: GetState")
      sender() ! instanceState(instance)

    case e @ TransitionFiredEvent(jobId, transitionId, timeStarted, timeCompleted, consumed, produced, output) =>
      log.debug(s"Received message: {}", e)
      log.debug(s"Transition '${topology.transitions.getById(transitionId)}' fired successfully")

      persistEvent(instance, e) { (updateInstance, e) =>
        step(updateInstance)
        sender() ! TransitionFired[S](transitionId, e.consumed, e.produced, instanceState(updateInstance))
      }

    case e @ TransitionFailedEvent(jobId, transitionId, timeStarted, timeFailed, consume, input, reason, strategy) =>
      log.debug(s"Received message: {}", e)
      log.warning(s"Transition '${topology.transitions.getById(transitionId)}' failed with: {}", reason)

      val updatedInstance = applyEvent(instance)(e)

      strategy match {
        case RetryWithDelay(delay) =>
          log.warning(
            s"Scheduling a retry of transition '${topology.transitions.getById(transitionId)}' in $delay milliseconds"
          )
          val originalSender = sender()
          system.scheduler.scheduleOnce(delay milliseconds) { executeJob(updatedInstance.jobs(jobId), originalSender) }
        case _ =>
      }

      sender() ! TransitionFailed(transitionId, consume, input, reason, strategy)
      context become running(updatedInstance)

    case msg @ FireTransition(id, input, correlationId) =>
      log.debug(s"Received message: {}", msg)

      fireTransitionById[S](id, input).run(instance).value match {
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

  def step(instance: Instance[S]) = {
    fireAllEnabledTransitions.run(instance).value match {
      case (updatedInstance, jobs) =>
        if (jobs.isEmpty && updatedInstance.activeJobs.isEmpty)
          settings.idleTTL.foreach { ttl =>
            system.scheduler.scheduleOnce(ttl, context.self, IdleStop(updatedInstance.sequenceNr))
          }

        jobs.foreach(job => executeJob(job, sender()))
        context become running(updatedInstance)
    }
  }

  def executeJob[E](job: Job[S, E], originalSender: ActorRef) =
    runJobAsync(job, executor)(settings.evaluationStrategy).unsafeRunAsyncFuture().pipeTo(context.self)(originalSender)

  override def onRecoveryCompleted(instance: Instance[S]) = step(instance)
}
