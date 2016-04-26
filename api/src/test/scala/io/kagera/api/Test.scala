package io.kagera.api

import io.kagera.api.colored._

object Test extends App {

  val initialMarking: Marking[Place] = Map.empty

  // This recursively executes the process until no transitions can be fired

  //  def stepRecursive(max: Int): Future[TransitionFired[_, _, _]] =
  //    (io.kagera.akka.actor ? Step).mapTo[TransitionFired[_, _, _]]
  //      .flatMap { result =>
  //        if (max <= 0) Future.successful(result)
  //        else stepRecursive(max - 1)
  //      }

  // This generated a .dot representation of the process, visualize using http://mdaines.github.io/viz.js/
  // val dot = TestProcesses.sum.innerGraph.toDot
  //  println(dot)
}
