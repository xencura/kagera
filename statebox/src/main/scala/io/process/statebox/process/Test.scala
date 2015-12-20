package io.process.statebox.process

import akka.actor.Props
import akka.util.Timeout
import io.process.statebox.ConfiguredActorSystem
import io.process.statebox.actor.{ PetriNetActor, PetriNetDebugging }
import PetriNetDebugging.Step
import io.process.statebox.process.colored._

import scala.concurrent.Await

object Test extends App with ConfiguredActorSystem {

  val a = Place[Int]("a")
  val b = Place[Int]("b")
  val c = Place[Int]("result")

  val init = TFn(id = 1, l = "init") { () =>
    (5, 5)
  }

  val sum = TFn(id = 2, l = "sum") { (a: Int, b: Int) =>
    a + b
  }

  val simpleProcess = process(init ~> %(a, b), %(a, b) ~> sum, sum ~> c)

  val initialMarking: Marking[Place] = Map.empty

  val actor = system.actorOf(Props(new PetriNetActor(simpleProcess, initialMarking)))

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  actor ! Step

  Await.result(actor ? Step, timeout.duration)
}
