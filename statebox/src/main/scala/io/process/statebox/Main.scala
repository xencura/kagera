package io.process.statebox

import spray.routing.HttpServiceActor

object Main extends App {

  // actor system for spray-can

  // actor system for statebox

  trait HttpApi extends HttpServiceActor with Services

  trait Services

  scala.sys.ShutdownHookThread {}
}
