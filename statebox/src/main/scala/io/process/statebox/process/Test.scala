package io.process.statebox.process

import akka.actor.Props
import akka.util.Timeout
import io.process.statebox.ConfiguredActorSystem
import io.process.statebox.actor.{ PetriNetActor, PetriNetDebugging }
import PetriNetDebugging.Step
import io.process.statebox.process.simple._
import io.process.statebox.process.colored._

import scala.concurrent.Await

object Test extends App with ConfiguredActorSystem {

  val initialMarking: Marking[Place] = Map.empty

  val actor = system.actorOf(Props(new PetriNetActor(TestProcesses.sum.sumProcess, initialMarking)))

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  actor ! Step

  Await.result(actor ? Step, timeout.duration)
}
