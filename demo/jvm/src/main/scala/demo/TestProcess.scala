package io.kagera.demo

import io.kagera.api.colored._
import io.kagera.api.colored.dsl._

import scala.concurrent.ExecutionContext

trait TestProcess {

  val p1 = Place[Unit](id = 1, label = "p1")
  val p2 = Place[Unit](id = 2, label = "p2")
  val p3 = Place[Unit](id = 3, label = "p3")

  val t1 = nullTransition(id = 1, label = "t1")
  val t2 = nullTransition(id = 2, label = "t2")

  def sequentialProcess(implicit executionContext: ExecutionContext) =
    process[Unit](p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)
}
