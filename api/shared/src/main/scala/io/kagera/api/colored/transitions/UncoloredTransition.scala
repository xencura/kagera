package io.kagera.api.colored.transitions

import cats.ApplicativeError
import cats.effect.Sync
import io.kagera.api.colored._
import io.kagera.api.multiset.MultiSet

trait UncoloredTransition[Input, Output, State] extends Transition[Input, Output, State] {

  override def apply[F[_]](inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
    sync: Sync[F],
    errorHandling: ApplicativeError[F, Throwable]
  ) = { (consume, state, input) =>
    {
      // assumes uncolored outgoing places (Place[Unit])
      val produce = outAdjacent.map { case (p, count) => p -> Map(() -> count) }.toMarking
      sync.map(produceEvent[F](consume, state, input))(output => (produce, output))
    }
  }

  def produceEvent[F[_] : Sync](consume: Marking, state: State, input: Input): F[Output]
}
