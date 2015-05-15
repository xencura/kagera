package io.process.statebox.http.routes

import io.process.statebox.common.ActorSystemProvider
import io.process.statebox.process.PetriNetActor.GetState
import spray.routing._
import akka.pattern.ask

trait ProcessRoutes extends Directives {

  self: ActorSystemProvider =>

  val processRoute: Route = path("process" / Segment / IntNumber) { (sn, id) =>
    get {
      val instance = system.actorSelection("")

      onComplete(instance ? GetState) { case Int =>
        complete("foo")
      }
    }
  }
}
