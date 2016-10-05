package io.kagera.demo

import akka.NotUsed
import akka.actor.{ Actor, ActorSystem }
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Source
import io.kagera.akka.persistence.TransitionFired
import io.kagera.api.colored._
import io.kagera.api.multiset._

trait Queries {

  this: ConfiguredActorSystem =>

  // obtain read journal
  val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // obtain all persistence ids
  val persistenceIds: Source[String, NotUsed] = queries.allPersistenceIds()

  persistenceIds.runForeach { id => println("id: " + id) }
}

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
