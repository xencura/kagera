package io.kagera.demo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Source

trait Queries {

  this: ConfiguredActorSystem =>

  // obtain read journal
  val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // obtain all persistence ids
  val persistenceIds: Source[String, NotUsed] = queries.allPersistenceIds()

  persistenceIds.runForeach { id => println("id: " + id) }
}
