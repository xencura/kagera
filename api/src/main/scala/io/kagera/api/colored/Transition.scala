package io.kagera.api.colored

import cats.{ Applicative, Functor }
import io.kagera.api.colored.ExceptionStrategy.BlockTransition
import io.kagera.api.multiset.MultiSet

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
 * A transition in a Colored Petri Net
 *
 * @tparam I
 *   The input type of the transition, the type of value that is required as input
 * @tparam O
 *   The output type of the transition, the type of value that this transition 'emits' or 'produces'
 * @tparam S
 *   The type of state the transition closes over.
 */
trait Transition[-I, O, S] {

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
   * The state event sourcing function.
   */
  def updateState: S => O => S
}
