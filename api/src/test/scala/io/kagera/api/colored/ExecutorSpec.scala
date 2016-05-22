package io.kagera.api.colored

import io.kagera.api.colored
import io.kagera.api._
import io.kagera.api.colored._
import colored.dsl._
import org.scalatest.WordSpec

import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.edge.WLDiEdge

class ExecutorSpec extends WordSpec {

  val p1 = nullPlace(1, "p1")
  val p2 = nullPlace(2, "p2")

  val mockedTransition = nullTransition(1, "t1", false)

  "A colored executor" should {
    "Call the transition with the appropriate parameters" in {

      implicit val ec: ExecutionContext = ExecutionContext.global

      val p = process(p1 ~> mockedTransition, mockedTransition ~> p2)

      val m: ColoredMarking = Map(p1 -> Seq(null))

      println(m.multiplicity)
      println(simple.findEnabledTransitions(p)(m.multiplicity))
      println(p.enabledParameters(m))
    }
  }

}
