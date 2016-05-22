package io.kagera.api.colored

import io.kagera.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

package object dsl {

  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit class TransitionDSL(t: Transition) {
    def ~>(p: Place, weight: Long = 1): Arc = arc(t, p, weight)
  }

  implicit class PlaceDSL(p: Place) {
    def ~>(t: Transition, weight: Long = 1): Arc = arc(p, t, weight)
  }

  def arc(t: Transition, p: Place, weight: Long): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, "")

  def arc(p: Place, t: Transition, weight: Long): Arc =
    WLDiEdge[Node, PTEdge](Left(p), Right(t))(weight, new PTEdgeImpl(weight, null))

  def noFilter(weight: Long): PTEdge = new PTEdgeImpl(weight, p => value => true)

  def nullPlace(id: Long, label: String) = Place[Null](id, label)

  def nullTransition(id: Long, label: String, isManaged: Boolean = false) =
    new AbstractTransition[Null, Null](id, label, isManaged, Duration.Undefined) {

      override def createOutput(output: Output, outAdjacent: Seq[(WLDiEdge[Node], Place)]): ColoredMarking =
        outAdjacent.map { case (arc, place) =>
          place -> List.fill(arc.weight.toInt)(null)
        }.toMap

      override def createInput(inAdjacent: Seq[(Place, WLDiEdge[Node], Seq[Any])], data: Option[Any]): Input = null

      override def apply(input: Input)(implicit executor: scala.concurrent.ExecutionContext): Future[Output] =
        Future.successful(null)
    }

  def process(params: Seq[Arc]*): PetriNetProcess[Place, Transition, ColoredMarking] =
    new ScalaGraphPetriNet(Graph(params.reduce(_ ++ _): _*)) with ColoredPetriNetProcess

  def processInstance(
    process: PetriNetProcess[Place, Transition, ColoredMarking],
    initialMarking: ColoredMarking = Map.empty
  ): ColoredPetriNetInstance =
    new ColoredPetriNetInstance(process, initialMarking)
}
