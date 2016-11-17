package io.kagera.akka.actor

import io.kagera.api.colored.{ ExceptionStrategy, Marking, Transition }

/**
 * Describes the messages to and from a PetriNetInstance actor.
 */
object PetriNetInstanceProtocol {

  /**
   * A common trait for all commands to a petri net instance.
   */
  sealed trait Command

  /**
   * Command to request the current state of the petri net instance.
   */
  case object GetState extends Command

  object Initialize {

    def apply(marking: Marking): Initialize[Unit] = Initialize[Unit](marking, ())
  }

  /**
   * Command to initialize a petri net instance.
   */
  case class Initialize[S](marking: Marking, state: S) extends Command

  object FireTransition {

    def apply[I](t: Transition[I, _, _], input: I): FireTransition = FireTransition(t.id, input, None)

    def apply(t: Transition[Unit, _, _]): FireTransition = FireTransition(t.id, (), None)
  }

  /**
   * Command to fire a specific transition with input.
   */
  case class FireTransition(transitionId: Long, input: Any, correlationId: Option[Long] = None) extends Command

  /**
   * A common trait for all reponses coming from a petri net instance.
   */
  sealed trait Response

  /**
   * A response indicating that the instance has been initialized in a certain state.
   */
  case class Initialized[S](marking: Marking, state: S) extends Response

  sealed trait TransitionResult extends Response

  /**
   * Response indicating that a transition has fired successfully
   */
  case class TransitionFired[S](transitionId: Long, consumed: Marking, produced: Marking, result: InstanceState[S])
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
   * The exception state of a transition.
   */
  case class ExceptionState(consecutiveFailureCount: Int, failureReason: String, failureStrategy: ExceptionStrategy)

  /**
   * Response containing the state of the process.
   */
  case class InstanceState[S](sequenceNr: BigInt, marking: Marking, state: S, failures: Map[Long, ExceptionState])
}
