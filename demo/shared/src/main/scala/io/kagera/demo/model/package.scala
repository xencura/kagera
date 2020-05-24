package io.kagera.demo

import upickle.default._

package object model {

  case class Place(id: Long, label: String)
  object Place {
    implicit val rw: ReadWriter[Place] = macroRW[Place]
  }

  case class Transition(id: Long, label: String, code: String = "", input: Option[String] = None)
  object Transition {
    implicit val rw: ReadWriter[Transition] = macroRW[Transition]
  }

  case class Edge(source: String, target: String, weigth: Int = 1)
  object Edge {
    implicit val rw: ReadWriter[Edge] = macroRW[Edge]
  }

  case class PetriNetModel(
                            places: Set[Place],
                            transitions: Set[Transition],
                            edges: Set[Edge]) {

    def nodes: Set[Either[Place, Transition]] = places.map(p => Left(p)) ++ transitions.map(t => Right(t))
  }
  object PetriNetModel {
    implicit val rw: ReadWriter[PetriNetModel] = macroRW[PetriNetModel]
  }

  case class ProcessState(marking: Map[Long, Int], data: Map[String, String])
}
