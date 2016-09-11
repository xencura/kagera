package io.kagera.demo

import akka.actor.Actor
import io.kagera.akka.persistence.TransitionFired
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.api.multiset._

class AggregateMarking[S](topology: ExecutablePetriNet[S]) extends Actor {

  override def receive: Receive = updateMarking(MultiSet.empty)

  def updateMarking(aggregateMarking: MultiSet[Long]): Receive = {

    case TransitionFired(Some(tid), Some(started), Some(completed), consumed, produced, data) =>
      val minusConsumed = consumed.foldLeft(aggregateMarking) { case (aggregate, token) =>
        aggregate.multisetDecrement(token.placeId.get, token.count.get)
      }

      val newAggregate = produced.foldLeft(minusConsumed) { case (aggregate, token) =>
        aggregate.multisetIncrement(token.placeId.get, token.count.get)
      }

      context become updateMarking(newAggregate)
  }
}
