package io.kagera.demo.http

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.util.{ ByteString, Timeout }
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.api.colored.{ ExecutablePetriNet, Generators }
import io.kagera.demo.ConfiguredActorSystem

trait Routes extends Directives {

  this: ConfiguredActorSystem =>

  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  val repository: Map[String, ExecutablePetriNet[_]] = Map("test" -> Generators.uncoloredSequential(5))

  val repositoryRoutes = pathPrefix("process") {
    path("_index") {
      complete(upickle.default.write(repository.keySet))
    } ~
      path(Segment) { id =>
        get {
          repository.get(id) match {
            case None => complete(StatusCodes.NotFound -> s"no such process: $id")
            case Some(process) => complete(upickle.default.write(Util.toModel(process)))
          }
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
