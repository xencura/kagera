package io.kagera.analyse

import akka.http.scaladsl.server.Directives

trait QueryRoutes extends Directives {

  // search processes by various means
  val searchRoutes = pathPrefix("query") {
    path("index") {
      get { complete("") }
    } ~
      path("current_by_marking") {
        // given a marking  (place -> count) returns all processes that currently have that state
        post { complete("") }
      } ~
      path("by_history") {
        // given a transition firing sequence (t1, t2, .. , tn) returns all processes that have this history
        post { complete("") }
      } ~
      path("by_topology") {
        // given a topology
        post { complete("") }
      }
  }
}
