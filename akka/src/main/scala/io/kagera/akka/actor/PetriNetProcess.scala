package io.kagera.akka.actor

import akka.actor.{ ActorLogging, ActorRef, Props }
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import akka.pattern.pipe
import io.kagera.akka.actor.PetriNetExecution.{ ExecutionState, Job }
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.akka.actor.PetriNetProcessProtocol._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored._

import scala.collection._
import scala.concurrent.duration._
import scala.language.existentials

object PetriNetProcess {

  def props[S](process: ExecutablePetriNet[S], initialStateProvider: String => (Marking, S)): Props =
    Props(new PetriNetProcess[S](process, initialStateProvider))

  def props[S](process: ExecutablePetriNet[S], initialMarking: Marking, initialState: S): Props =
    props(process, id => (initialMarking, initialState))

  sealed trait TransitionEvent

  /**
   * An event describing the fact that a transition has fired in the petri net process.
   */
  case class TransitionFiredEvent(
    jobId: Long,
    transitionId: Long,
    timeStarted: Long,
    timeCompleted: Long,
    consumed: Marking,
    produced: Marking,
    out: Any
  ) extends TransitionEvent

  /**
   * An event describing the fact that a transition failed to fire.
   */
  case class TransitionFailedEvent(
    jobId: Long,
    transitionId: Long,
    timeStarted: Long,
    timeFailed: Long,
    consume: Marking,
    input: Any,
    failureReason: String,
    exceptionStrategy: ExceptionStrategy
  ) extends TransitionEvent
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetProcess[S](process: ExecutablePetriNet[S], initialStateFn: String => (Marking, S))
    extends PersistentActor
    with ActorLogging
    with PetriNetActorRecovery[S] {

  val processId = context.self.path.name

  override def persistenceId: String = s"process-$processId"

  import context.dispatcher

  override def receiveCommand = Map.empty

  def running(state: ExecutionState[S]): Receive = {
    case GetState =>
      sender() ! state.processState

    case e @ TransitionFiredEvent(jobId, transitionId, timeStarted, timeCompleted, consumed, produced, output) =>
      persist(writeEvent(e)) { persisted =>
        log.debug(s"Transition fired ${transitionId}")
        val updatedState = state.apply(e)
        executeAllEnabledTransitions(state.apply(e))
        sender() ! TransitionFired[S](transitionId, e.consumed, e.produced, updatedState.marking, updatedState.state)
      }

    case e @ TransitionFailedEvent(
          jobId,
          transitionId,
          timeStarted,
          timeFailed,
          consume,
          input,
          reason,
          strategy @ RetryWithDelay(delay)
        ) =>
      log.warning(s"Transition '${transitionId}' failed: {}", reason)

      log.info(s"Scheduling a retry of transition ${transitionId} in $delay milliseconds")
      val originalSender = sender()
      system.scheduler.scheduleOnce(delay milliseconds) { executeJob(state.jobs(jobId), originalSender) }

      sender() ! TransitionFailed(transitionId, consume, input, reason, strategy)

    case e @ TransitionFailedEvent(jobId, transitionId, timeStarted, timeFailed, consume, input, reason, strategy) =>
      log.warning(s"Transition '${transitionId}' failed: {}", reason)
      sender() ! TransitionFailed(transitionId, consume, input, reason, strategy)

    case FireTransition(id, input, correlationId) =>
      log.debug(s"Received message to fire transition $id with input: $input")

      process.findTransitionById(id) match {
        case Some(transition) =>
          state.fireTransition(transition, input) match {
            case (_, Right(notEnabledReason)) =>
              sender() ! TransitionNotEnabled(transition, notEnabledReason)
            case (newState, Left(job)) =>
              executeJob(job, sender())
              context become running(newState)
          }
        case None =>
          val msg = s"No transition exists with id: $id"
          sender() ! TransitionNotEnabled(id, msg)
          log.warning(msg)
      }
  }

  def executeAllEnabledTransitions(state: ExecutionState[S]) = {
    val (newState, jobs) = state.fireAllEnabledTransitions()
    jobs.foreach(job => executeJob(job, sender()))
    context become running(newState)
  }

  def executeJob[E](job: Job[S, E], originalSender: ActorRef) = job.run().pipeTo(context.self)(originalSender)

  override def initialState = initialStateFn(processId) match { case (marking, state) => (process, marking, state) }

  override def onRecoveryCompleted(state: ExecutionState[S]) = executeAllEnabledTransitions(state)
}
