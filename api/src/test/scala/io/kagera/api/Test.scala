package io.kagera.api

import io.kagera.api.colored._
import shapeless.{ HList, HNil }
import shapeless._

object Test extends App {

  val initialMarking: Marking[Place] = Map.empty

  type Foo = Int :: String :: HNil

  def sumfn(a: Int, b: Int): Int = a + b

  val inputA = 4 :: 2 :: HNil
  val inputB = true :: "foo" :: HNil

  val a = inputA ++ inputB

  val out = (sumfn _).tupled.apply(inputA.tupled)

  println(a)

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
