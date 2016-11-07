package io.kagera.akka.actor

import akka.actor.{ ActorLogging, ActorRef, Props }
import akka.pattern.pipe
import akka.persistence.PersistentActor
import fs2.Strategy
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.execution.EventSourcing._
import io.kagera.execution._

import scala.concurrent.duration._
import scala.language.existentials

object PetriNetInstance {

  def props[S](topology: ExecutablePetriNet[S]): Props = Props(
    new PetriNetInstance[S](topology, new TransitionExecutorImpl[S](topology))
  )
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetInstance[S](override val topology: ExecutablePetriNet[S], executor: TransitionExecutor[S])
    extends PersistentActor
    with ActorLogging
    with PetriNetInstanceRecovery[S] {

  val processId = context.self.path.name

  override def persistenceId: String = s"process-$processId"

  import context.dispatcher

  override def receiveCommand = uninitialized

  def uninitialized: Receive = { case msg @ Initialize(marking, state) =>
    log.debug(s"Received message: {}", msg)
    persistEvent(Instance.uninitialized(topology), InitializedEvent(marking, state.asInstanceOf[S])) {
      (updatedState, e) =>
        executeAllEnabledTransitions(updatedState)
        sender() ! Initialized(marking, state)
    }
  }

  def running(instance: Instance[S]): Receive = {
    case GetState =>
      log.debug(s"Received message: GetState")

      sender() ! InstanceState[S](instance.sequenceNr, instance.marking, instance.state)

    case e @ TransitionFiredEvent(jobId, transitionId, timeStarted, timeCompleted, consumed, produced, output) =>
      log.debug(s"Received message: {}", e)

      persistEvent(instance, e) { (updateInstance, e) =>
        executeAllEnabledTransitions(updateInstance)
        sender() ! TransitionFired[S](
          transitionId,
          e.consumed,
          e.produced,
          updateInstance.marking,
          updateInstance.state
        )
      }

    case e @ TransitionFailedEvent(jobId, transitionId, timeStarted, timeFailed, consume, input, reason, strategy) =>
      log.debug(s"Received message: {}", e)
      log.warning(s"Transition '${topology.transitions.getById(transitionId)}' failed with: {}", reason)

      val updatedInstance = updateInstance(instance)(e)

      strategy match {
        case RetryWithDelay(delay) =>
          log.warning(s"Scheduling a retry of transition ${transitionId} in $delay milliseconds")
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
  }

  def executeAllEnabledTransitions(instance: Instance[S]) = {
    fireAllEnabledTransitions.run(instance).value match {
      case (updatedInstance, jobs) =>
        jobs.foreach(job => executeJob(job, sender()))
        context become running(updatedInstance)
    }
  }

  // TODO: best to use another thread pool
  implicit val s: Strategy = Strategy.fromExecutionContext(context.dispatcher)

  def executeJob[E](job: Job[S, E], originalSender: ActorRef) =
    runJob(job, executor).unsafeRunAsyncFuture().pipeTo(context.self)(originalSender)

  override def onRecoveryCompleted(instance: Instance[S]) = executeAllEnabledTransitions(instance)
}
