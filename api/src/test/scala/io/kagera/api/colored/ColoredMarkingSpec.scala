package io.kagera.api.colored

import org.scalatest.Matchers._
import org.scalatest.WordSpec

class ColoredMarkingSpec extends WordSpec {

  case class Person(name: String, age: Int)

  val p1 = Place[Int](id = 1, label = "p1")
  val p2 = Place[String](id = 2, label = "p2")
  val p3 = Place[Double](id = 3, label = "p3")
  val p4 = Place[Person](id = 4, label = "p4")

  "A Colored Marking" should {

    "correctly implement the multiplicity function" in {

      val m = ColoredMarking(p1(1, 2), p2("foo", "bar"))

      m.multiplicities shouldBe Map(p1 -> 2, p2 -> 2)
    }

    "have correct produce semantics" in {

      val m1 = ColoredMarking(p1(1, 2), p2("foo", "bar"))

      m1 ++ ColoredMarking.empty shouldBe m1

      val m2 = ColoredMarking(p1(3), p2("baz"), p3(1d))

      m1 ++ m2 shouldBe ColoredMarking(p1(3, 1, 2), p2("baz", "foo", "bar"), p3(1d))
    }

    "have correct consume semantics" in {

      val m1: ColoredMarking = ColoredMarking(p1(1, 2, 3), p2("foo", "bar"), p4(Person("Joe", 42)))

      m1 -- ColoredMarking.empty shouldBe m1

      val m2 = ColoredMarking(p1(2), p4(Person("Joe", 42)))

      m1 -- m2 shouldBe ColoredMarking(p1(1, 3), p2("foo", "bar"))
    }

    "in case of token value equality only consume tokens equal to the multiplicity" in {

      val m1 = ColoredMarking(p1(1, 1, 1, 1, 1))
      val m2 = ColoredMarking(p1(1, 1))

      m1 -- m2 shouldBe ColoredMarking(p1(1, 1, 1))
    }
  }
}