package io.kagera.api.colored.transitions

import cats.Functor
import cats.syntax.functor._
import io.kagera.api.colored._
import io.kagera.api.multiset.MultiSet

abstract class UncoloredTransitionExecutorFactory[F[_] : Functor, T] extends TransitionExecutorFactory[F, T] {

  override def createTransitionExecutor(
    t: T,
    inAdjacent: MultiSet[Place[_]],
    outAdjacent: MultiSet[Place[_]]
  ): TransitionFunctionF[F, Input, Output, State] = { (consume, state, input) =>
    {
      // assumes uncolored outgoing places (Place[Unit])
      val produce = outAdjacent.map { case (p, count) => p -> Map(() -> count) }.toMarking
      produceEvent(t, consume, state, input).map(output => (produce, output))
    }
  }

  def produceEvent(t: T, consume: Marking, state: State, input: Input): F[Output]
}
