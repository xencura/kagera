package io.kagera.api.colored

import io.kagera.api._
import io.kagera.api.colored.dsl._
import org.scalatest.Matchers._
import org.scalatest.WordSpec

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scalax.collection.edge.WLDiEdge

class ColoredMarkingSpec extends WordSpec {

  val p1 = Place[Int](id = 1, label = "p1")
  val p2 = Place[String](id = 2, label = "p2")
  val p3 = Place[Double](id = 3, label = "p3")

  val t1 = new AbstractTransition[Any, Any](4, "t1", isManaged = false) {

    override def createOutput(output: Output, outAdjacent: Seq[(WLDiEdge[Node], Place)]): ColoredMarking =
      outAdjacent.map { case (edge, place) =>
        place -> Seq(output)
      }.toMap

    override def createInput(
      inAdjacent: Seq[(Place, WLDiEdge[Node], Seq[Any])],
      data: Option[Any],
      context: TransitionContext
    ): Input = data.orNull

    override def apply(input: Input)(implicit executor: scala.concurrent.ExecutionContext): Future[Output] =
      Future.successful(input)
  }

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

    "should only contain the last state" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(null))

      val petriNet = process(p1 ~> t1, t1 ~> p2)
      val instance = processInstance(petriNet, initialMarking, java.util.UUID.randomUUID())

      instance.marking shouldBe initialMarking

      Await.result(instance.fireTransition(t1), 2 seconds) shouldBe Map(p2 -> Seq(null))
    }

    "should contain all accumulated state (even is marking is already consumed)" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(null))

      val petriNet = process(p1 ~> t1, t1 ~> p2)
      val instance = processInstance(petriNet, initialMarking, java.util.UUID.randomUUID())

      instance.marking shouldBe initialMarking
      Await.result(instance.fireTransition(t1), 2 seconds)
      instance.accumulatedMarking shouldBe Map(p1 -> Seq(null), p2 -> Seq(null))
    }

    "should maintain old state when a place is overwritten with new data" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(1, 2))
      val extraData = 3

      val petriNet = process(p1 ~> t1, t1 ~> p2)

      val instance = processInstance(petriNet, initialMarking, java.util.UUID.randomUUID())

      instance.marking shouldBe initialMarking
      Await.result(instance.fireTransition(t1, Some(extraData)), 2 seconds)
      instance.accumulatedMarking shouldBe Map(p1 -> Seq(1, 2), p2 -> Seq(3))
    }
  }
}
