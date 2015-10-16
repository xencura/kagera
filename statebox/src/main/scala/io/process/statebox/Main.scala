package io.process.statebox

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import io.process.statebox.common.{ ActorSystemProvider, DefaultSettingsProvider }
import io.process.statebox.http.Routes

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

trait ServicesImpl extends ActorSystemProvider {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher
}

trait HttpApi extends ServicesImpl with Routes with DefaultSettingsProvider

object Main extends App with HttpApi {

  implicit val materializer = ActorMaterializer()

  Http().bindAndHandle(helloWorld, settings.http.interface, settings.http.port)

  scala.sys.ShutdownHookThread {
    // Do some clean up?
  }
}
