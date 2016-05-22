package io.kagera.api.colored

import io.kagera.api.colored.dsl._
import org.scalatest.Matchers._
import org.scalatest.WordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class AutoStepperSpec extends WordSpec {

  val p1 = Place[Null](id = 1, label = "p1")
  val p2 = Place[Null](id = 2, label = "p2")
  val p3 = Place[Null](id = 3, label = "p3")

  val t1 = nullTransition(1, "t1", isManaged = false)
  val t2 = nullTransition(2, "t2", isManaged = true)

  "The automatic stepper" should {

    "Automatically fire a transition if it is managed" in {

      val initialMarking: ColoredMarking = Map(p1 -> Seq(null))

      val petriNet = process(p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)

      val instance = processInstance(petriNet, initialMarking)

      instance.marking shouldBe initialMarking

      Await.result(instance.fireTransition(t1), 2 seconds) shouldBe Map(p3 -> Seq(null))
    }
  }
}
