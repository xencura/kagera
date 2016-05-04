package io.kagera.api.colored

import io.kagera.api.{ PetriNetInstance, PetriNetProcess, TokenGame }
import io.kagera.api.ScalaGraph.ScalaGraphPetriNet

import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

package object dsl {

  implicit val ec: ExecutionContext = ExecutionContext.global

  def arc(t: Transition, p: Place, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, fieldName)

  def arc(p: Place, t: Transition, weight: Long, fieldName: String): Arc =
    WLDiEdge[Node, String](Left(p), Right(t))(weight, fieldName)

  def process(params: Seq[Arc]*): PetriNetProcess[Place, Transition, ColoredMarking] =
    new ScalaGraphPetriNet(Graph(params.reduce(_ ++ _): _*)) with ColoredPetriNetProcess

  def processInstance(
    process: PetriNetProcess[Place, Transition, ColoredMarking]
  ): PetriNetInstance[Place, Transition, ColoredMarking] = {

    new PetriNetInstance[Place, Transition, ColoredMarking] {

      var currentMarking: ColoredMarking = Map.empty

      override def marking: ColoredMarking = currentMarking

      override def fireTransition(t: Transition, data: Option[Any]): Future[ColoredMarking] = {
        process.fireTransition(currentMarking)(t, data).flatMap(stepManagedRecursive)
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
