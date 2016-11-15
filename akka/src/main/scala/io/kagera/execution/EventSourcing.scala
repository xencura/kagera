package io.kagera.execution

import cats.data.State
import io.kagera.api._
import io.kagera.api.colored._

import scala.collection.Map

object EventSourcing {

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

  def applyEvent[S](e: Event): State[Instance[S], Unit] = State.modify { instance =>
    e match {
      case e: InitializedEvent[_] =>
        Instance[S](instance.process, 1, e.marking, e.state.asInstanceOf[S], Map.empty)
      case e: TransitionFiredEvent =>
        val t = instance.process.transitions.getById(e.transitionId).asInstanceOf[Transition[_, Any, S]]
        val newState = t.updateState(instance.state)(e.out)
        instance.copy(
          sequenceNr = instance.sequenceNr + 1,
          marking = (instance.marking |-| e.consumed) |+| e.produced,
          state = newState,
          jobs = instance.jobs - e.jobId
        )
      case e: TransitionFailedEvent =>
        val job = instance.jobs(e.jobId)
        val failureCount = job.failureCount + 1
        val updatedJob =
          job.copy(failure = Some(ExceptionState(e.transitionId, failureCount, e.failureReason, e.exceptionStrategy)))
        instance.copy(jobs = instance.jobs + (job.id -> updatedJob))
    }
  }
}
