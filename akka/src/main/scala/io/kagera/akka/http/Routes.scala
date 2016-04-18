package io.kagera.akka.http

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import io.kagera.akka.actor.PetriNetActor.GetState

trait Routes extends Directives {

  import akka.pattern.ask

  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  def statebox: ActorRef

  def getState(id: String) = statebox.ask(id -> GetState).mapTo[String]

  val routes = pathPrefix("api") {

    path("process" / Segment) { id =>
      pathEndOrSingleSlash {
        get { complete(getState(id)) }
      }
      (post & path("step")) { complete("stepping") }
    }
  }
}
