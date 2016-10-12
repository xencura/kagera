package io.kagera.demo.http

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.util.{ ByteString, Timeout }
import demo.http.StaticPages
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.api.colored.{ ExecutablePetriNet, Generators, Place, Transition }
import io.kagera.demo.{ model, ConfiguredActorSystem, TestProcess }
import io.kagera.dot.GraphDot
import upickle.default._

trait Routes extends Directives with TestProcess {

  this: ConfiguredActorSystem =>

  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  val repository: Map[String, ExecutablePetriNet[_]] = Map("test" -> Generators.uncoloredSequential(5))

  import io.kagera.dot.PetriNetDot._

  val dot = GraphDot.generateDot(repository.head._2.innerGraph, petriNetTheme[Place[_], Transition[_, _, _]])

  def toModel(pn: ExecutablePetriNet[_]): model.PetriNetModel = {

    val places = pn.nodes.collect { case Left(p) =>
      model.Place(p.id, p.label)
    }

    val transitions = pn.nodes.collect { case Right(t) =>
      model.Transition(t.id, t.label)
    }

    def nodeId(n: Either[Place[_], Transition[_, _, _]]): String = n match {
      case Left(p) => p.label
      case Right(t) => t.label
    }

    val graph = pn.innerGraph

    val edges = graph.edges.map { (e: graph.EdgeT) =>
      model.Edge(nodeId(e.source.value), nodeId(e.target.value), 1)
    }

    model.PetriNetModel(places.toSet, transitions.toSet, edges.toSet)
  }

  val repositoryRoutes = path("process" / Segment) { id =>
    get {
      repository.get(id) match {
        case None => complete(StatusCodes.NotFound -> s"no such process: $id")
        case Some(process) => complete(upickle.default.write(toModel(process)))
      }
    }
  }

  val dashBoardRoute = path("dashboard") {
    get {
      complete {
        HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, ByteString(StaticPages.dashboard.render)))
      }
    }
  }

  val resources = pathPrefix("resources") { getFromResourceDirectory("") }

  val processRoutes = pathPrefix("process") {

    path("_create") {
      post {
        complete("foo")
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
                  case success: TransitionFired[_] => "success"
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
