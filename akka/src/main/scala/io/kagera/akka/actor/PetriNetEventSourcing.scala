package io.kagera.akka.actor

import io.kagera.akka.actor.PetriNetExecution.{ ExceptionState, Instance, InstanceState }
import io.kagera.api.colored.{ ExceptionStrategy, Marking }

import scala.collection.Map

object PetriNetEventSourcing {

  sealed trait Event

  sealed trait TransitionEvent extends Event

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

  case class InitializedEvent[S](marking: Marking, state: S) extends Event

  def applyEvent[S](e: Event): InstanceState[S, Unit] = state =>
    e match {
      case e: InitializedEvent[_] =>
        (Instance[S](state.process, 1, e.marking, e.state.asInstanceOf[S], Map.empty), ())
      case e: TransitionFiredEvent =>
        val t = state.process.getTransitionById(e.transitionId)
        val newState = t.updateState(state.state)(e.out)
        val updatedInstance = state.copy(
          sequenceNr = state.sequenceNr + 1,
          marking = state.marking -- e.consumed ++ e.produced,
          state = newState,
          jobs = state.jobs - e.jobId
        )
        (updatedInstance, ())
      case e: TransitionFailedEvent =>
        val job = state.jobs(e.jobId)
        val failureCount = job.failureCount + 1
        val updatedJob =
          job.copy(failure = Some(ExceptionState(e.transitionId, failureCount, e.failureReason, e.exceptionStrategy)))
        val updatedInstance = state.copy(jobs = state.jobs + (job.id -> updatedJob))
        (updatedInstance, ())
    }
}
