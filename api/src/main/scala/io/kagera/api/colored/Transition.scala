package io.kagera.api.colored

import io.kagera.api.multiset.MultiSet

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * A transition in a Colored Petri Net
 *
 * @tparam Input
 *   The input type of the transition, the type of value that is required as input
 * @tparam Output
 *   The output type of the transition, the type of value that this transition 'emits' or 'produces'
 * @tparam State
 *   The type of state the transition closes over.
 */
trait Transition[Input, Output, State] {

  /**
   * The unique identifier of this transition.
   *
   * @return
   *   The unique identifier.
   */
  val id: Long

  /**
   * A human readable label of this transition.
   *
   * @return
   *   The label.
   */
  val label: String

  /**
   * Flag indicating whether this transition is managed or manually triggered from outside.
   *
   * This should be true iff Input == Unit.
   *
   * TODO how to encode this? the problem is in some contexts the Input type is unknown but this property might still be
   * needed
   *
   * Require a TypeClass?
   */
  val isManaged: Boolean

  /**
   * The maximum duration this transition may spend doing computation / io.
   */
  val maximumOperationTime: Duration

  /**
   * A duration, specifying the delay that the transition, after becoming enabled, may fire.
   *
   * @return
   */
  def delay: FiniteDuration = Duration.Zero

  /**
   * Given the in and out adjacent places with their weight returns a function:
   *
   * (Mi, S, I) => (Mo, O)
   *
   * Where:
   *
   * Mi is the in-adjacent marking, the tokens this transition consumes. S is the context state. I is input data
   *
   * Mo is the out-adjacent marking, the tokens this transition produces. O is the emitted output
   *
   * @param inAdjacent
   * @param outAdjacent
   * @param executor
   * @return
   */
  def apply(inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
    executor: scala.concurrent.ExecutionContext
  ): (ColoredMarking, State, Input) => Future[(ColoredMarking, Output)]

  /**
   * The state transition function:
   *
   * Given a value of type Output returns a function
   *
   * @param e
   * @return
   */
  def updateState(e: State): Output => State
}
