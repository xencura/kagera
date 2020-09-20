package io.kagera.api.colored

import io.kagera.api.colored.dsl._

import scala.concurrent.ExecutionContext

object Generators {

  object Uncolored {
    def sequence(nrOfSteps: Int, automated: Boolean = false): ExecutablePetriNet[Unit] = {

      val transitions = (1 to nrOfSteps).map(i => nullTransition(id = i, automated = automated))
      val places = (1 to (nrOfSteps - 1)).map(i => Place[Unit](id = i))
      val tpedges = transitions.zip(places).map { case (t, p) => arc(t, p, 1) }
      val ptedges = places.zip(transitions.tail).map { case (p, t) => arc(p, t, 1) }

      process[Unit]((tpedges ++ ptedges): _*)
    }
  }
}
