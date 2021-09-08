package io.kagera.api.colored

import io.kagera.api.multiset.MultiSet

import scala.reflect.ClassTag

abstract class TransitionExecutorFactory[F[_], T : ClassTag] {
  type Input
  type Output
  type State

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
  def createTransitionExecutor(
    t: T,
    inAdjacent: MultiSet[Place[_]],
    outAdjacent: MultiSet[Place[_]]
  ): TransitionFunctionF[F, Input, Output, State]

  /**
   * Choose the first succeeding decoder.
   */
  final def or[T2 >: T : ClassTag, S](
    d: => TransitionExecutorFactory.WithInputOutputState[F, T2, Input, Output, S]
  ): TransitionExecutorFactory.WithInputOutputState[F, T2, Input, Output, _] = new TransitionExecutorFactory[F, T2] {
    self =>
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
     * For example some type T[_, _, _] we have T => TransitionFunction The same goes for other properties defined on
     * this trait.
     *
     * This way we can forgo with the entire Transition trait and let user's use whatever they want. For example, in
     * simple cases (uncolored place / transition nets) an identifier (Int or Long) as a type is enough.
     *
     * @param inAdjacent
     * @param outAdjacent
     * @return
     */
    override def createTransitionExecutor(
      t: T2,
      inAdjacent: MultiSet[Place[_]],
      outAdjacent: MultiSet[Place[_]]
    ): TransitionFunctionF[F, Input, Output, State] = t match {
      case t: T => self.createTransitionExecutor(t, inAdjacent, outAdjacent)
      case t: T2 => d.createTransitionExecutor(t, inAdjacent, outAdjacent)
      case t => throw new RuntimeException(s"Don't know how to create a TransitionExecutor for $t")
    }
  }
}

object TransitionExecutorFactory {
  type WithState[F[_], T, S] = TransitionExecutorFactory[F, T] {
    type State = S
  }
  type WithInputOutputState[F[_], T, I, O, S] = TransitionExecutorFactory[F, T] {
    type Input >: I
    type Output = O
    type State = S
  }
}
