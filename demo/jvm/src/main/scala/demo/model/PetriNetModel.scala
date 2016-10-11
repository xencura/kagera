package io.kagera.demo.model

case class Place(id: Long, label: String)

case class Transition(id: Long, label: String)

case class PetriNetModel(places: Set[Place], transitions: Set[Transition], edges: Seq[(Long, Long, Int)]) {}
