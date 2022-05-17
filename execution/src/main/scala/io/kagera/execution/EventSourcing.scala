/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.kagera.execution

import cats.data.State
import io.kagera.api._
import io.kagera.api.colored._

import scala.collection.immutable.Map

object EventSourcing {

  sealed trait Event

  sealed trait TransitionEvent extends Event {
    def transitionId: Long
  }

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
  case class UpdatedInstance[S](instance: Instance[S]) extends Event
  case class TokenUpdatedInPlaceEvent[T](from: T, to: T, place: Place[T]) extends Event
  def applyEvent[S](e: Event): State[Instance[S], Unit] = State.modify { instance =>
    e match {
      case InitializedEvent(initialMarking, initialState) =>
        Instance[S](instance.process, 1, initialMarking, initialState.asInstanceOf[S], Map.empty)
      case UpdatedInstance(instance) => instance.asInstanceOf[Instance[S]]
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
      case e: TokenUpdatedInPlaceEvent[_] =>
        instance.copy(marking = instance.marking.updateIn(e.place, e.from, e.to))
    }
  }
}
