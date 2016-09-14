package io.kagera.api.colored

import io.kagera.api.colored
import io.kagera.api._
import io.kagera.api.colored._
import colored.dsl._
import org.scalatest.WordSpec

import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.edge.WLDiEdge

class ExecutorSpec extends WordSpec {

  val p1 = Place[Unit](id = 1, label = "p1")
  val p2 = Place[Unit](id = 2, label = "p2")

  val mockedTransition = nullTransition(1, "t1", false)

  "A colored executor" should {
    "Call the transition with the appropriate parameters" in {

      import scala.concurrent.ExecutionContext.Implicits.global

      val p = process(p1 ~> mockedTransition, mockedTransition ~> p2)

      val m = Marking(p1(()))
    }
  }
}
