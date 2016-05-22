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

  implicit class PlaceDSL[C](p: Place { type Color = C }) {
    def ~>(t: Transition, weight: Long = 1, filter: C => Boolean = token => true): Arc = arc[C](p, t, weight, filter)
  }

  def arc(t: Transition, p: Place, weight: Long): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, "")

  def arc[C](p: Place, t: Transition, weight: Long, filter: C => Boolean = (token: C) => true): Arc = {
    val innerEdge = new PTEdgeImpl[C](weight, filter)
    WLDiEdge[Node, PTEdge[C]](Left(p), Right(t))(weight, innerEdge)
  }

  def nullPlace(id: Long, label: String) = Place[Null](id, label)

  def constantTransition[I, O](id: Long, label: String, isManaged: Boolean = false, constant: O) =
    new AbstractTransition[Null, O](id, label, isManaged, Duration.Undefined) {

      override def createOutput(output: Output, outAdjacent: Seq[(WLDiEdge[Node], Place)]): ColoredMarking =
        outAdjacent.map { case (arc, place) =>
          place -> List.fill(arc.weight.toInt)(output)
        }.toMap

      override def createInput(inAdjacent: Seq[(Place, WLDiEdge[Node], Seq[Any])], data: Option[Any]): Input = null

      override def apply(input: Input)(implicit executor: scala.concurrent.ExecutionContext): Future[Output] =
        Future.successful(constant)
    }

  def nullTransition(id: Long, label: String, isManaged: Boolean = false) =
    constantTransition[Null, Null](id, label, isManaged, null)

  def process(params: Arc*): PetriNetProcess[Place, Transition, ColoredMarking] =
    new ScalaGraphPetriNet(Graph(params: _*)) with ColoredPetriNetProcess

  def processInstance(
    process: PetriNetProcess[Place, Transition, ColoredMarking],
    initialMarking: ColoredMarking = Map.empty
  ): ColoredPetriNetInstance =
    new ColoredPetriNetInstance(process, initialMarking)
}
