package io.kagera.api.colored

import io.kagera.api.PetriNetProcess
import io.kagera.api.ScalaGraph.ScalaGraphPetriNet

import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

package object dsl {

  def arc(t: Transition, p: Place, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, fieldName)

  def arc(p: Place, t: Transition, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Left(p), Right(t))(weight, fieldName)

  def process(params: Seq[Arc]*): PetriNetProcess[Place, Transition, ColoredMarking] =
    new ScalaGraphPetriNet(Graph(params.reduce(_ ++ _): _*)) with ColoredPetriNetProcess
}
