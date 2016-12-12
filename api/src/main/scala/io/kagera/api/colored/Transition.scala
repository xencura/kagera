package io.kagera.api.colored

import io.kagera.api.colored.ExceptionStrategy.BlockTransition
import io.kagera.api.multiset.MultiSet

import scala.concurrent.duration.Duration

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
   * This is only true IFF Input == Unit.
   *
   * TODO How to encode this? the problem is in some contexts the Input type is unknown but this property might still be
   * needed
   *
   * Require a TypeTag for Input?
   */
  val isAutomated: Boolean

  /**
   * The maximum duration this transition may spend doing computation / io.
   */
  val maximumOperationTime: Duration

  /**
   * Indicates a strategy to use when dealing with exceptions.
   *
   * @return
   */
  def exceptionStrategy: TransitionExceptionHandler = (e, n) => BlockTransition

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
   * TODO instead of requiring this on a Transition trait a Type-Class approach looks more flexible.
   *
   * For example some type T[_, _, _] we have T => TransitionFunction The same goes for other properties defined on this
   * trait.
   *
   * This way we can forgo with the entire Transition trait and let user's use whatever they want. For example, in
   * simple cases (uncolored place / transition nets) an identifier (Int or Long) as a type is enough.
   *
   * @param inAdjacent
   * @param outAdjacent
   * @return
   */
  def apply(inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]]): TransitionFunction[Input, Output, State]

  /**
   * The state event sourcing function.
   */
  def updateState: State => Output => State
}
