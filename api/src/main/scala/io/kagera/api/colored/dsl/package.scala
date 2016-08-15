package io.kagera.api.colored

import io.kagera.api._
import io.kagera.api.colored.ColoredMarking.MarkingData
import io.kagera.api.multiset._

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

package object dsl {

  def stateFunction[S, E](stateTransition: S => E => S, id: Long = Random.nextLong, isManaged: Boolean = false)(
    fn: S => E
  ): Transition[Unit, E, S] =
    new AbstractTransition[Unit, E, S](id, label = "", isManaged, maximumOperationTime = Duration.Undefined) {

      override def apply(inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
        executor: ExecutionContext
      ) =
        (consume, state, in) => {

          val produce: MarkingData = outAdjacent.map { case (place, count) =>
            place -> Map(() -> count)
          }.toMap

          Future.successful(ColoredMarking(produce), fn(state))
        }

      override def updateState(state: S): (E) => S = stateTransition(state)
    }

  implicit class TransitionDSL(t: Transition[_, _, _]) {
    def ~>[C](p: Place[C], weight: Long = 1): Arc = arc(t, p, weight)
  }

  implicit class PlaceDSL[C](p: Place[C]) {
    def ~>(t: Transition[C, _, _], weight: Long = 1, filter: C => Boolean = token => true): Arc =
      arc[C](p, t, weight, filter)
  }

  def arc(t: Transition[_, _, _], p: Place[_], weight: Long): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, "")

  def arc[C](p: Place[C], t: Transition[_, _, _], weight: Long, filter: C => Boolean = (token: C) => true): Arc = {
    val innerEdge = new PTEdgeImpl[C](weight, filter)
    WLDiEdge[Node, PTEdge[C]](Left(p), Right(t))(weight, innerEdge)
  }

  def nullPlace(id: Long, label: String) = Place[Unit](id, label)

  def constantTransition[I, O, S](id: Long, label: String, isManaged: Boolean = false, constant: O) =
    new IdentityTransition[I, O, S](id, label, isManaged, Duration.Undefined) {
      override def apply(inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
        executor: ExecutionContext
      ): (ColoredMarking, S, I) => Future[(ColoredMarking, O)] = { (marking, state, input) =>
        {
          val produced: MarkingData = outAdjacent.map { case (place, weight) =>
            place -> produceTokens(place, weight.toInt)
          }.toMap

          Future.successful(ColoredMarking(produced) -> constant)
        }
      }

      def produceTokens[C](place: Place[C], count: Int): MultiSet[C] =
        MultiSet.empty[C] + (constant.asInstanceOf[C] -> count)
    }

  def nullTransition[S](id: Long, label: String, isManaged: Boolean = false) =
    constantTransition[Unit, Unit, S](id, label, isManaged, ())

  def process[S](params: Arc*)(implicit ec: ExecutionContext): ColoredPetriNetProcess[S] =
    new ScalaGraphPetriNet(Graph(params: _*)) with ColoredTokenGame with TransitionExecutor[S] {
      override val executionContext = ec
    }
}
