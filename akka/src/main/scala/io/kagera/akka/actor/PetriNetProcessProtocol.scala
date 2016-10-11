package io.kagera.akka.actor

import io.kagera.api.colored.{ ExceptionStrategy, Marking, Transition }

object PetriNetProcessProtocol {

  // commands
  trait Command

  /**
   * Command to request the next enabled transition to be fired.
   */
  case object Step extends Command

  /**
   * Command to request the current state of the process.
   */
  case object GetState extends Command

  object FireTransition {

    def apply[I](t: Transition[I, _, _], input: I): FireTransition = FireTransition(t.id, input, None)

    def apply(t: Transition[Unit, _, _]): FireTransition = FireTransition(t.id, (), None)
  }

  /**
   * Command to fire a specific transition with input.
   */
  case class FireTransition(transition_id: Long, input: Any, correlationId: Option[Long] = None) extends Command

  // responses
  sealed trait TransitionResult

  /**
   * Response indicating that a transition has fired successfully
   */
  case class TransitionFired[S](transition_id: Long, consumed: Marking, produced: Marking, marking: Marking, state: S)
      extends TransitionResult

  /**
   * Response indicating that a transition has failed.
   */
  case class TransitionFailed(
    transition_id: Long,
    consume: Marking,
    input: Any,
    reason: String,
    strategy: ExceptionStrategy
  ) extends TransitionResult

  /**
   * Response indicating that the transition could not be fired because it is not enabled.
   */
  case class TransitionNotEnabled(transition_id: Long, reason: String) extends TransitionResult

  /**
   * Response containing the state of the process.
   */
  case class ProcessState[S](sequenceNr: BigInt, marking: Marking, state: S)
}
