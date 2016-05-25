package io.kagera.api.colored

import io.kagera.api._
import org.scalatest.Matchers._
import org.scalatest.WordSpec

class ColoredMarkingSpec extends WordSpec {

  val p1 = Place[Int](id = 1, label = "p1")
  val p2 = Place[String](id = 2, label = "p2")
  val p3 = Place[Double](id = 3, label = "p3")

  "A Colored Marking" should {

    "correctly implement the multiplicity function" in {

      val m: ColoredMarking = Map(p1 -> Seq(1, 2), p2 -> Seq("foo", "bar"))
      m.multiplicity shouldBe Map(p1 -> 2, p2 -> 2)
    }

    "have correct produce semantics" in {

      val m1: ColoredMarking = Map(p1 -> Seq(1, 2), p2 -> Seq("foo", "bar"))

      m1.produce(Map.empty) shouldBe m1

      val m2: ColoredMarking = Map(p1 -> Seq(3), p2 -> Seq("baz"), p3 -> Seq(1d))

      m1.produce(m2) shouldBe Map(p1 -> Seq(3, 1, 2), p2 -> Seq("baz", "foo", "bar"), p3 -> Seq(1d))
    }

    "have correct consume semantics" in {

      val m1: ColoredMarking = Map(p1 -> Seq(1, 2, 3), p2 -> Seq("foo", "bar"), p3 -> Seq(1.1d))

      m1.consume(Map.empty) shouldBe m1

      val m2: ColoredMarking = Map(p1 -> Seq(2), p3 -> Seq(1.1d))

      m1.consume(m2) shouldBe Map(p1 -> Seq(1, 3), p2 -> Seq("foo", "bar"))
    }

    "in case of token value equality only consume tokens equal to the multiplicity" in {

      val m1: ColoredMarking = Map(p1 -> Seq(1, 1, 1, 1, 1))
      val m2: ColoredMarking = Map(p1 -> Seq(1, 1))

      m1.consume(m2) shouldBe Map(p1 -> Seq(1, 1, 1))
    }
  }
}
