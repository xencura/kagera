package io.kagera.demo.http

import io.kagera.api.colored.{ Place, Transition, _ }
import io.kagera.demo.model

object Util {
  def toModel(pn: ExecutablePetriNet[_]): model.PetriNetModel = {

    val places = pn.nodes.collect {
      case Left(p) => model.Place(p.id, p.label)
    }

    val transitions = pn.nodes.collect {
      case Right(t) => model.Transition(t.id, t.label)
    }

    def nodeId(n: Either[Place[_], Transition[_, _, _]]): String = n match {
      case Left(p)  => p.label
      case Right(t) => t.label
    }

    val graph = pn.innerGraph

    val edges = graph.edges.map { (e: graph.EdgeT) =>
      model.Edge(nodeId(e.source.value), nodeId(e.target.value), 1)
    }

    model.PetriNetModel(places.toSet, transitions.toSet, edges.toSet)
  }
}
