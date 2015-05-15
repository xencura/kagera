package io.process.statebox.http

import io.process.statebox.common.ActorSystemProvider
import io.process.statebox.http.routes.ProcessRoutes
import spray.routing._

trait Routes extends HttpService with ProcessRoutes {

  self: ActorSystemProvider =>

  val routes = processRoute
}
