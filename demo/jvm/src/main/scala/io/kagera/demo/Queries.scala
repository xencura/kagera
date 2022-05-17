/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.kagera.demo

import akka.NotUsed
import akka.actor.Actor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Source
import io.kagera.api.colored._
import io.kagera.api.multiset._
import io.kagera.persistence.messages.TransitionFired

trait Queries {

  this: ConfiguredActorSystem =>

  // obtain read journal
  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  def allProcessIds: Source[String, NotUsed] = readJournal.currentPersistenceIds()

  def journalFor(id: String): Source[String, NotUsed] = {
    readJournal.currentEventsByPersistenceId(s"process-$id", 0, Long.MaxValue).map { e =>
      e.event.toString
    }
  }

}

class AggregateMarking[S](topology: ExecutablePetriNet[S]) extends Actor {

  override def receive: Receive = updateMarking(MultiSet.empty)

  def updateMarking(aggregateMarking: MultiSet[Long]): Receive = {

    case TransitionFired(_, _, Some(tid), Some(started), Some(completed), consumed, produced, data, _) =>
      val minusConsumed = consumed.foldLeft(aggregateMarking) { case (aggregate, token) =>
        aggregate.multisetDecrement(token.placeId.get, token.count.get)
      }

      val newAggregate = produced.foldLeft(minusConsumed) { case (aggregate, token) =>
        aggregate.multisetIncrement(token.placeId.get, token.count.get)
      }

      context become updateMarking(newAggregate)
  }
}
