package io.process.statebox

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import io.process.statebox.common.{ ActorSystemProvider, DefaultSettingsProvider }
import io.process.statebox.http.Routes

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

trait ConfiguredActorSystem extends ActorSystemProvider {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  implicit val materializer = ActorMaterializer()
}

trait HttpApi extends ConfiguredActorSystem with Routes with DefaultSettingsProvider

object Main extends App with HttpApi {

  Http().bindAndHandle(helloWorld, settings.http.interface, settings.http.port)

  scala.sys.ShutdownHookThread {
    // Do some clean up?
  }
}
