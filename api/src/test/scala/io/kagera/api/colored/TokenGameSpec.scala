package io.kagera.api.colored

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import io.kagera.api.colored.dsl._

class TokenGameSpec extends WordSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  val p1 = Place[Int](id = 1, label = "p1")
  val p2 = Place[Int](id = 2, label = "p2")

  val t1 = constantTransition[Int, Int, Unit](id = 4, label = "t1", false, 42)
  val t2 = constantTransition[Int, Int, Unit](id = 5, label = "t2", false, 5)

  val testProcess = process(p1 ~> (t1, filter = _ > 42), p1 ~> (t2, weight = 3), t1 ~> p2, t2 ~> p2)

  "The Colored Token game" should {

    "NOT mark a transition enabled if there are NOT enough tokens in in-adjacent places" in {

      val marking = Marking(p1(10, 10))

      // the multiplicity of p1 is 2
      marking.multiplicities(p1) should be(2)

      // t2 requires at least 3 tokens in p1 and is therefor not enabled
      testProcess.isEnabled(marking)(t2) should be(false)
    }

    "DO mark a transition as enabled if there ARE enough tokens in in-adjacent places" in {
      val marking = Marking(p1(10, 10, 10))

      // the multiplicity of p1 is 2
      marking.multiplicities(p1) should be(3)

      // t2 requires at least 3 tokens in p1 and is therefor not enabled
      testProcess.isEnabled(marking)(t2) should be(true)
    }

    "NOT mark a transition enabled if an in-adjacent edge filters the token" in {

      val marking = Marking(p1(10))

      testProcess.isEnabled(marking)(t1) should be(false)
    }
  }
}
