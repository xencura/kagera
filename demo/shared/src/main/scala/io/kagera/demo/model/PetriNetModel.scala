package io.kagera.demo.model

case class Place(id: Long, label: String)

case class Transition(id: Long, label: String)

case class Edge(source: String, target: String, weigth: Int = 1)

case class PetriNetModel(places: Set[Place], transitions: Set[Transition], edges: Set[Edge]) {

  def nodes: Set[Either[Place, Transition]] = places.map(p => Left(p)) ++ transitions.map(t => Right(t))
}
