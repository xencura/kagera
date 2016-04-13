package io.process.statebox.process

import akka.actor.Props
import akka.util.Timeout
import io.process.statebox.ConfiguredActorSystem
import io.process.statebox.actor.PetriNetActor.TransitionFired
import io.process.statebox.actor.{ PetriNetActor, PetriNetDebugging }
import PetriNetDebugging.Step
import io.process.statebox.process.simple._
import io.process.statebox.process.colored._
import ScalaGraph._

import scala.concurrent.{ Await, Future }

object Test extends App with ConfiguredActorSystem {

  val initialMarking: Marking[Place] = Map.empty

  val actor = system.actorOf(Props(new PetriNetActor(TestProcesses.sum, initialMarking)))

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(2 seconds)

  // This recursively executes the process until no transitions can be fired
  Await.result(stepRecursive(10), timeout.duration)

  def stepRecursive(max: Int): Future[TransitionFired[_, _, _]] =
    (actor ? Step)
      .mapTo[TransitionFired[_, _, _]]
      .flatMap { result =>
        if (max <= 0) Future.successful(result)
        else stepRecursive(max - 1)
      }

  // This generated a .dot representation of the process, visualize using http://mdaines.github.io/viz.js/
  // val dot = TestProcesses.sum.innerGraph.toDot
  //  println(dot)
}
