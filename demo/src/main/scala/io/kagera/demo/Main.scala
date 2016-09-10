package io.kagera.demo
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import io.kagera.demo.http.Routes

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success }

trait ConfiguredActorSystem {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer = ActorMaterializer()
}

trait HttpApi extends ConfiguredActorSystem with Routes

object Main extends App with HttpApi {

  val interface: String = "localhost"
  val port: Int = 8080

  val bind = Http().bindAndHandle(routes, interface, port)
  val httpBind = s"http://${interface}:${port}"

  bind.onComplete {
    case Success(result) => println(s"Successfully bound to: $httpBind")
    case Failure(reason) => println(s"Failed to bind to: $httpBind")
  }

  scala.sys.ShutdownHookThread {
    // Do some clean up?
  }

  override def stateActor: ActorRef = ???
}
