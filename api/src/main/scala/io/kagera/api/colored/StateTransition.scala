package io.kagera.api.colored

import io.kagera.api.multiset.{ MultiSet, _ }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

abstract class StateTransition[I, E, S](id: Long, label: String, isManaged: Boolean, duration: Duration)
    extends AbstractTransition[I, E, S](id, label, isManaged, duration) {

  override def apply(inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
    executor: ExecutionContext
  ): (ColoredMarking, S, I) => Future[(ColoredMarking, E)] = { (consume, state, input) =>
    {
      val produce = outAdjacent.map {
        // assumes uncolored outgoing places (Place[Unit])
        case (p, count) => p -> MultiSet.from(List.fill(count)(()))
      }.toMarking

      produceEvent(consume, state, input).map(output => (produce, output))
    }
  }

  def produceEvent(consume: ColoredMarking, state: S, input: I): Future[E]
}
