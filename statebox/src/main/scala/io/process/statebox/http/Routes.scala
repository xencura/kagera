package io.process.statebox.http

import akka.http.scaladsl.server.Directives

trait Routes extends Directives {

  val helloWorld =
    (get & path("hello")) { complete("hello world") }
}
