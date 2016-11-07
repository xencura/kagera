package io.kagera.akka.actor

import io.kagera.api.colored.{ ExceptionStrategy, Marking, Transition }

object PetriNetInstanceProtocol {

  // commands
  trait Command

  /**
   * Command to request the current state of the process.
   */
  case object GetState extends Command

  case class Initialize[S](marking: Marking, state: S) extends Command

  object FireTransition {

    def apply[I](t: Transition[I, _, _], input: I): FireTransition = FireTransition(t.id, input, None)

    def apply(t: Transition[Unit, _, _]): FireTransition = FireTransition(t.id, (), None)
  }

  /**
   * Command to fire a specific transition with input.
   */
  case class FireTransition(transitionId: Long, input: Any, correlationId: Option[Long] = None) extends Command

  // responses
  case class Initialized[S](marking: Marking, state: S)

  sealed trait TransitionResult

  /**
   * Response indicating that a transition has fired successfully
   */
  case class TransitionFired[S](transitionId: Long, consumed: Marking, produced: Marking, marking: Marking, state: S)
      extends TransitionResult

  /**
   * Response indicating that a transition has failed.
   */
  case class TransitionFailed(
    transitionId: Long,
    consume: Marking,
    input: Any,
    reason: String,
    strategy: ExceptionStrategy
  ) extends TransitionResult

  /**
   * Response indicating that the transition could not be fired because it is not enabled.
   */
  case class TransitionNotEnabled(transitionId: Long, reason: String) extends TransitionResult

  /**
   * Response containing the state of the process.
   */
  case class InstanceState[S](sequenceNr: BigInt, marking: Marking, state: S)
}
