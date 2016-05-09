package io.kagera.api.colored

import io.kagera.api.{ PetriNetInstance, PetriNetProcess, TokenGame }
import io.kagera.api.ScalaGraph.ScalaGraphPetriNet

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

  def nullTransition(_id: Long, _label: String, _isManaged: Boolean = false) = new Transition {

    override val isManaged: Boolean = _isManaged
    override val label: String = _label
    override val id: Long = _id

    override type Output = Null
    override type Input = Null

    override def createOutput(output: Output, outAdjacent: Seq[(WLDiEdge[Node], Place)]): ColoredMarking =
      outAdjacent.map { case (arc, place) =>
        place -> Seq(null)
      }.toMap

    override def createInput(inAdjacent: Seq[(Place, WLDiEdge[Node], Seq[Any])], data: Option[Any]): Input = null
    override def apply(input: Input): Future[Output] = {
      println(s"firing transition $label")
      Future.successful(null)
    }
  }

  def process(params: Seq[Arc]*): PetriNetProcess[Place, Transition, ColoredMarking] =
    new ScalaGraphPetriNet(Graph(params.reduce(_ ++ _): _*)) with ColoredPetriNetProcess

  def processInstance(
    process: PetriNetProcess[Place, Transition, ColoredMarking],
    initialMarking: ColoredMarking = Map.empty
  ): PetriNetInstance[Place, Transition, ColoredMarking] = {

    new PetriNetInstance[Place, Transition, ColoredMarking] {

      var currentMarking: ColoredMarking = initialMarking

      override def marking: ColoredMarking = currentMarking

      override def fireTransition(t: Transition, data: Option[Any]): Future[ColoredMarking] = {
        process.fireTransition(currentMarking)(t, data).flatMap(stepManagedRecursive).map { marking =>
          currentMarking = marking
          marking
        }
      }

      // this is very dangerous hack, could go into an infinite recursive loop
      def stepManagedRecursive(marking: ColoredMarking): Future[ColoredMarking] = {
        process
          .enabledTransitions(marking)
          .filter(_.isManaged)
          .headOption
          .map(t => process.fireTransition(marking)(t, None).flatMap(stepManagedRecursive))
          .getOrElse(Future.successful(marking))
      }

      override def step(): Future[ColoredMarking] = ???
    }
  }
}
