package io.kagera.akka

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import io.kagera.akka.common.{ ActorSystemProvider, DefaultSettingsProvider }
import io.kagera.akka.http.Routes

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success }

trait ConfiguredActorSystem extends ActorSystemProvider {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  implicit val materializer = ActorMaterializer()
}

trait HttpApi extends ConfiguredActorSystem with Routes with DefaultSettingsProvider

object Main extends App with HttpApi {

  val bind = Http().bindAndHandle(routes, settings.http.interface, settings.http.port)
  val httpBind = s"http://${settings.http.interface}:${settings.http.port}"

  bind.onComplete {
    case Success(result) => println(s"Successfully bound to: $httpBind")
    case Failure(reason) => println(s"Failed to bind to: $httpBind")
  }

  scala.sys.ShutdownHookThread {
    // Do some clean up?
  }

  override def statebox: ActorRef = ???
}
