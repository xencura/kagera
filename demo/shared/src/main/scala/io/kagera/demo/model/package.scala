package io.kagera.demo

package object model {

  case class Place(id: Long, label: String)

  case class Transition(id: Long, label: String, code: String = "", input: Option[String] = None)

  case class Edge(source: String, target: String, weigth: Int = 1)

  case class PetriNetModel(places: Set[Place], transitions: Set[Transition], edges: Set[Edge]) {

    def nodes: Set[Either[Place, Transition]] = places.map(p => Left(p)) ++ transitions.map(t => Right(t))
  }

  case class ProcessState(marking: Map[Long, Int], data: Map[String, String])
}
