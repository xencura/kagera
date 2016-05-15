package io.kagera.api.colored

import io.kagera.api.PetriNetProcess
import io.kagera.api.ScalaGraph.ScalaGraphPetriNet

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

package object dsl {

  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit class TransitionDSL(t: Transition) {
    def ~>(p: Place, weight: Long = 1, label: String = ""): Arc =
      WLDiEdge[Node, String](Right(t), Left(p))(weight, label)
  }

  implicit class PlaceDSL(p: Place) {
    def ~>(t: Transition, weight: Long = 1, label: String = ""): Arc =
      WLDiEdge[Node, String](Left(p), Right(t))(weight, label)
  }

  def arc(t: Transition, p: Place, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, "")

  def arc(p: Place, t: Transition, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Left(p), Right(t))(weight, "")

  def nullPlace(id: Long, label: String) = Place[Null](id, label)

  def nullTransition(id: Long, label: String, isManaged: Boolean = false) =
    new TransitionImpl[Null, Null](id, label, isManaged, Duration.Undefined) {

      override def createOutput(output: Output, outAdjacent: Seq[(WLDiEdge[Node], Place)]): ColoredMarking =
        outAdjacent.map { case (arc, place) =>
          place -> List.fill(arc.weight.toInt)(null)
        }.toMap

      override def createInput(inAdjacent: Seq[(Place, WLDiEdge[Node], Seq[Any])], data: Option[Any]): Input = null
      override def apply(input: Input): Future[Output] = Future.successful(null)
    }

  def process(params: Seq[Arc]*): PetriNetProcess[Place, Transition, ColoredMarking] =
    new ScalaGraphPetriNet(Graph(params.reduce(_ ++ _): _*)) with ColoredPetriNetProcess

  def processInstance(
    process: PetriNetProcess[Place, Transition, ColoredMarking],
    initialMarking: ColoredMarking = Map.empty
  ) =
    new ColoredPetriNetInstance(process, initialMarking)
}
