package io.kagera.api.colored

import io.kagera.api.multiset.MultiSet

import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * A transition in a colored petri net.
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
   * Given the in and out adjacent places with their weight returns a function:
   *
   * (Mi, S, I) => (Mo, S', O)
   *
   * Where:
   *
   * Mi is the in-adjacent marking, the tokens this transition consumes. S is the context state. I is input data
   *
   * Mo is the out-adjacent marking, the tokens this transition produces. S' is the changed context state. O is the
   * emitted output
   *
   * @param inAdjacent
   * @param outAdjacent
   * @param executor
   * @return
   */
  def apply(inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
    executor: scala.concurrent.ExecutionContext
  ): (ColoredMarking, State, Input) => Future[(ColoredMarking, Output)]

  def updateState(e: Output): State => State
}
