package io.kagera.demo.http

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import io.kagera.akka.actor.PetriNetProcess
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.api.colored.{ ColoredMarking, ExecutablePetriNet }
import io.kagera.demo.{ ConfiguredActorSystem, TestProcess }
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.Source

trait Routes extends Directives with TestProcess {

  this: ConfiguredActorSystem =>

  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  val repository: Map[String, ExecutablePetriNet[_]] = Map("test" -> sequentialProcess)

  val repositoryRoutes = pathPrefix("repository") {

    path("index") {
      // returns a pageable index of all topologies
      get { complete("") }
    }
  }

  // obtain read journal
  val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // obtain all persistence ids
  val persistenceIds: Source[String, NotUsed] = queries.allPersistenceIds()

  //  val allEvents = persistenceIds.flatMapConcat(id => queries.eventsByPersistenceId(id, 0L, Long.MaxValue))

  persistenceIds.runForeach { id => println("id: " + id) }

  val processRoutes = pathPrefix("process") {

    path("_create") {
      post {

        val id = java.util.UUID.randomUUID().toString
        val props = PetriNetProcess.props(sequentialProcess, ColoredMarking.empty, ())
        system.actorOf(props, id).path.name

        complete(id)
      }
    } ~
      path(Segment) { id =>
        {

          val actorSelection = system.actorSelection(s"/user/$id")

          pathEndOrSingleSlash {
            get {
              // should return the current state (marking) of the process
              val futureResult = actorSelection.ask(GetState).mapTo[State[_]].map { state =>
                state.marking.toString
              }
              complete(futureResult)
            }
          } ~
            path("fire" / Segment) { tid =>
              post {
                val msg = FireTransition(tid.toLong, ())
                val futureResult = actorSelection.ask(msg).mapTo[TransitionResult].map {
                  case success: TransitionFiredSuccessfully[_] => "success"
                  case failure: TransitionFailed => "failure"
                }
                complete(futureResult)
              }
            }

          path("step") {
            // should attempt to fire the next enabled transition
            post { complete("") }
          }
        }
      }

  }
}
