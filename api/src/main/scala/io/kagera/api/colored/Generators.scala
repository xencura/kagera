package io.kagera.api.colored

import dsl._

import scala.concurrent.ExecutionContext

object Generators {

  def uncoloredSequential(nrOfSteps: Int, interactive: Boolean = true)(implicit
    executionContext: ExecutionContext
  ): ExecutablePetriNet[Unit] = {
    val transitions = (1 to nrOfSteps).map(i => nullTransition(id = i, label = s"t$i", isManaged = !interactive))
    val places = (1 to nrOfSteps).map(i => Place[Unit](id = i, label = s"p$i"))

    val tpedges = transitions.zip(places).map { case (t, p) => arc(t, p, 1) }

    val ptedges = places.zip(transitions.tail).map { case (p, t) => arc(p, t, 1) }

    process[Unit]((tpedges ++ ptedges): _*)
  }
}
