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
    output: Option[Any]
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
    input: Option[Any],
    failureReason: String,
    exceptionStrategy: ExceptionStrategy
  ) extends TransitionEvent

  /**
   * An event describing the fact that an instance was initialized.
   */
  case class InitializedEvent(marking: Marking, state: Any) extends Event

  def applyEvent[S](e: Event): State[Instance[S], Unit] = State.modify { instance =>
    e match {
      case InitializedEvent(initialMarking, initialState) =>
        Instance[S](instance.process, 1, initialMarking, initialState.asInstanceOf[S], Map.empty)
      case e: TransitionFiredEvent =>
        val t = instance.transitionById(e.transitionId).get.asInstanceOf[Transition[_, Any, S]]
        val newState = e.output.map(t.updateState(instance.state)).getOrElse(instance.state)

        instance.copy(
          sequenceNr = instance.sequenceNr + 1,
          marking = (instance.marking |-| e.consumed) |+| e.produced,
          state = newState,
          jobs = instance.jobs - e.jobId
        )
      case e: TransitionFailedEvent =>
        val job = instance.jobs.getOrElse(
          e.jobId,
          Job[S, Any](
            e.jobId,
            instance.state,
            instance.transitionById(e.transitionId).asInstanceOf[Transition[Any, Any, S]],
            e.consume,
            e.input,
            None
          )
        )
        val failureCount = job.failureCount + 1
        val updatedJob =
          job.copy(failure = Some(ExceptionState(e.transitionId, failureCount, e.failureReason, e.exceptionStrategy)))
        instance.copy(jobs = instance.jobs + (job.id -> updatedJob))
    }
  }
}
