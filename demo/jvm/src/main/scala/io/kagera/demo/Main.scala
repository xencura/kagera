/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.kagera.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import io.kagera.demo.http.Routes

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success }

trait ConfiguredActorSystem {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer = ActorMaterializer()
}

object Main extends App with ConfiguredActorSystem with Routes {

  val interface: String = "localhost"
  val port: Int = 8080

  val bind = Http().newServerAt(interface, port).bindFlow(indexRoute ~ topologyRoutes ~ resourceRoutes ~ processRoutes)
  val httpBind = s"http://${interface}:${port}"

  bind.onComplete {
    case Success(result) => println(s"Successfully bound to: $httpBind")
    case Failure(reason) => println(s"Failed to bind to: $httpBind")
  }

  scala.sys.ShutdownHookThread {
    // Do some clean up?
  }
}
