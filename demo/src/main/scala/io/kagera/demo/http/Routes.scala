package io.kagera.demo.http

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import io.kagera.akka.actor.PetriNetProcess.GetState
import io.kagera.api.colored.ExecutablePetriNet

trait Routes extends Directives {

  import akka.pattern.ask

  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  def stateActor: ActorRef
  def getState(id: String) = stateActor.ask(id -> GetState).mapTo[String]

  val repository: Map[String, ExecutablePetriNet[_]] = Map.empty

  val repositoryRoutes = pathPrefix("repository") {

    path("index") {
      // returns a pageable index of all topologies
      get { complete("") }
    }
  }

  // search processes by various means
  val searchRoutes = pathPrefix("search") {
    path("index") {
      get { complete("") }
    } ~
      path("current_by_marking") {
        // given a marking  (place -> count) returns all processes that currently have that state
        post { complete("") }
      } ~
      path("by_history") {
        // given a transition firing sequence (t1, t2, .. , tn) returns all processes that have this history
        post { complete("") }
      } ~
      path("by_topology") {
        // given a topology
        post { complete("") }
      }
  }

  val processRoutes = pathPrefix("process") {
    pathEndOrSingleSlash {
      path(Segment) { id =>
        {
          pathEndOrSingleSlash {
            get {
              // should return the current state (marking) of the process
              complete("")
            }
          } ~
            path("step") {
              // should attempt to fire the next enabled transition
              post { complete("") }
            }
        }
      }
    }
  }
}
