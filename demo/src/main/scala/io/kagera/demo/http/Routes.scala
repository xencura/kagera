package io.kagera.demo.http

import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.util.Timeout
import io.kagera.akka.actor.PetriNetProcess
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.api.colored.{ ExecutablePetriNet, Generators, Marking, Place, Transition }
import io.kagera.demo.{ ConfiguredActorSystem, TestProcess }
import io.kagera.dot.GraphDot

trait Routes extends Directives with TestProcess {

  this: ConfiguredActorSystem =>

  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  val repository: Map[String, ExecutablePetriNet[_]] = Map("test" -> Generators.uncoloredSequential(5))

  import io.kagera.dot.PetriNetDot._

//  val dot = GraphDot.generateDot(repository.head._2.innerGraph, petriNetTheme[Place[_], Transition[_, _, _]])

  val repositoryRoutes = pathPrefix("repository") {

    path("index") {
      // returns a pageable index of all topologies
      get { complete("") }
    }
  }

  val processRoutes = pathPrefix("process") {

    path("_create") {
      post {

        val id = java.util.UUID.randomUUID().toString
        val props = PetriNetProcess.props(sequentialProcess, Marking.empty, ())
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
