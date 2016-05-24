package io.kagera.api.colored

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import io.kagera.api.colored.dsl._

class TokenGameSpec extends WordSpec {

  val p1 = Place[Int](id = 1, label = "p1")
  val p2 = Place[Int](id = 2, label = "p2")

  val t1 = constantTransition[Int, Int](id = 4, label = "t1", false, 42)
  val t2 = constantTransition[Int, Int](id = 5, label = "t2", false, 5)

  val testProcess = process(p1 ~> (t1, filter = _ > 42), p1 ~> (t2, weight = 3), t1 ~> p2, t2 ~> p2)

  "The Colored Token game" should {

    "not mark a transition enabled if there are not enough tokens in in-adjacent places" in {

      val marking: ColoredMarking = Map(p1 -> Seq(10, 10))

      // t2 requires at least 3 tokens in p1
      testProcess.isEnabled(marking)(t2) should be(false)
    }

    "not mark a transition enabled if an in-adjacent edge filters the token" in {

      val marking: ColoredMarking = Map(p1 -> Seq(10))

      testProcess.isEnabled(marking)(t1) should be(false)
    }
  }
}
