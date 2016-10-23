package io.kagera.api.colored

import io.kagera.api.colored
import io.kagera.api._
import io.kagera.api.colored._
import colored.dsl._
import org.scalatest.WordSpec

import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.edge.WLDiEdge

class ExecutorSpec extends WordSpec {

  val p1 = Place[Unit](id = 1)
  val p2 = Place[Unit](id = 2)

  val mockedTransition = nullTransition(id = 1, automated = false)

  "A colored executor" should {
    "Call the transition with the appropriate parameters" in {

      val p = process(p1 ~> mockedTransition, mockedTransition ~> p2)

      val m = Marking(p1(()))
    }
  }
}
