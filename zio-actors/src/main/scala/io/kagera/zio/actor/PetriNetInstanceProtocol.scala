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

package io.kagera.zio.actor

import io.kagera.api.colored.{ ExceptionStrategy, Marking, Place, Transition }
import zio.stream.Stream

/**
 * Describes the messages to and from a PetriNetInstance actor.
 */
object PetriNetInstanceProtocol {

  /**
   * A common trait for all commands to a petri net instance.
   */
  sealed trait Message[+_]

  /**
   * Command to request the current state of the petri net instance.
   */
  case class GetState[S]() extends Message[InstanceState[S]]
  case class UpdateTokenInPlace[T](from: T, to: T, place: Place[T]) extends Message[InstanceState[_]]
  case class AssignTokenToPlace[T](token: T, place: Place[T]) extends Message[InstanceState[_]]

  /**
   * Command to initialize a petri net instance.
   */
  case class SetMarkingAndState[S](marking: Marking, state: S) extends Message[Response] with HasMarking
  object SetMarkingAndState {
    def apply(marking: Marking): SetMarkingAndState[Unit] = SetMarkingAndState[Unit](marking, ())
  }

  /**
   * Command to fire a specific transition with input.
   */
  case class FireTransition(transitionId: Long, input: Any, correlationId: Option[Long] = None)
      extends Message[Stream[Throwable, TransitionResponse]]
  object FireTransition {
    def apply[I](t: Transition[I, _, _], input: I): FireTransition = FireTransition(t.id, input, None)
    def apply(t: Transition[Unit, _, _]): FireTransition = FireTransition(t.id, (), None)
  }

  case class IdleStop(seq: Long) extends Message[Unit]

  /**
   * A common trait for all responses coming from a petri net instance.
   */
  sealed trait Response

  /**
   * Response indicating that the command could not be processed because of the current state of the actor.
   *
   * This message is only send in response to Command messages.
   */
  case class IllegalCommand(reason: String) extends Response

  /**
   * A response indicating that the instance has been initialized in a certain state.
   *
   * This message is only send in response to an Initialize message.
   */
  case class Initialized[S](marking: Marking, state: S) extends Response with HasMarking

  /**
   * Any message that is a response to a FireTransition command.
   */
  sealed trait TransitionResponse extends Response {
    val transitionId: Long
  }

  /**
   * Response indicating that a transition has fired successfully
   */
  case class TransitionFired[S](
    override val transitionId: Long,
    consumed: Marking,
    produced: Marking,
    result: InstanceState[S]
  ) extends TransitionResponse

  /**
   * Response indicating that a transition has failed.
   */
  case class TransitionFailed(
    override val transitionId: Long,
    consume: Marking,
    input: Any,
    reason: String,
    strategy: ExceptionStrategy
  ) extends TransitionResponse

  /**
   * Response indicating that the transition could not be fired because it is not enabled.
   */
  case class TransitionNotEnabled(override val transitionId: Long, reason: String) extends TransitionResponse

  /**
   * The exception state of a transition.
   */
  case class ExceptionState(failureCount: Int, failureReason: String, failureStrategy: ExceptionStrategy)
      extends Response

  /**
   * Response containing the state of the process.
   */
  case class InstanceState[S](
    sequenceNr: Long,
    marking: Marking,
    state: Option[S],
    failures: Map[Long, ExceptionState],
    enabledTransitions: Set[Transition[_, _, _]]
  ) extends Response
      with HasMarking {

    def hasFailed(transitionId: Long): Boolean = failures.contains(transitionId)
  }
  trait HasMarking {
    def marking: Marking
  }
}
