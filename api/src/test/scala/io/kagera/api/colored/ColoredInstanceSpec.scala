package io.kagera.api.colored

import io.kagera.api.colored.dsl._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import scala.concurrent.Await
import scala.concurrent.duration._
import scalax.collection.edge.WLDiEdge

class ColoredInstanceSpec extends WordSpec {

  val p1 = nullPlace(id = 1, label = "p1")
  val p2 = nullPlace(id = 2, label = "p2")

  val t1 = new IdentityTransition[Any](4, "t1", isManaged = false) {

    override def createOutput(output: Output, outAdjacent: Seq[(WLDiEdge[Node], Place)]): ColoredMarking =
      outAdjacent.map { case (edge, place) =>
        place -> Seq(output)
      }.toMap

    override def createInput(
      inAdjacent: Seq[(Place, WLDiEdge[Node], Seq[Any])],
      data: Option[Any],
      context: TransitionContext
    ): Input = data.orNull
  }

  val petriNet = process(p1 ~> t1, t1 ~> p2)

  "A ColoredPetriNetInstance" should {

    "after being created be in the initial marking state" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(null))
      val instance = processInstance(petriNet, initialMarking, java.util.UUID.randomUUID())
      instance.marking shouldBe initialMarking
    }

    "only contain the last state" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(null))
      val instance = processInstance(petriNet, initialMarking, java.util.UUID.randomUUID())

      Await.result(instance.fireTransition(t1), 2 seconds) shouldBe Map(p2 -> Seq(null))
    }

    "maintain all accumulated state (even is marking is already consumed)" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(null))

      val instance = processInstance(petriNet, initialMarking, java.util.UUID.randomUUID())

      Await.result(instance.fireTransition(t1), 2 seconds)
      instance.accumulatedMarking shouldBe Map(p1 -> Seq(null), p2 -> Seq(null))
    }

    "maintain old state when a place is overwritten with new data" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(1, 2))
      val extraData = 3

      val instance = processInstance(petriNet, initialMarking, java.util.UUID.randomUUID())

      Await.result(instance.fireTransition(t1, Some(extraData)), 2 seconds)
      instance.accumulatedMarking shouldBe Map(p1 -> Seq(1, 2), p2 -> Seq(3))
    }
  }
}
