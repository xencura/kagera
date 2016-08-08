package io.kagera.api.colored

import io.kagera.api._
import io.kagera.api.multiset._

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

package object dsl {

  implicit class TransitionDSL(t: Transition) {
    def ~>[C](p: Place[C], weight: Long = 1): Arc = arc(t, p, weight)
  }

  implicit class PlaceDSL[C](p: Place[C]) {
    def ~>(t: Transition, weight: Long = 1, filter: C => Boolean = token => true): Arc = arc[C](p, t, weight, filter)
  }

  def arc[C](t: Transition, p: Place[C], weight: Long): Arc =
    WLDiEdge[Node, String](Right(t), Left(p))(weight, "")

  def arc[C](p: Place[C], t: Transition, weight: Long, filter: C => Boolean = (token: C) => true): Arc = {
    val innerEdge = new PTEdgeImpl[C](weight, filter)
    WLDiEdge[Node, PTEdge[C]](Left(p), Right(t))(weight, innerEdge)
  }

  def nullPlace(id: Long, label: String) = Place[Null](id, label)

  def constantTransition[I, O](id: Long, label: String, isManaged: Boolean = false, constant: O) =
    new AbstractTransition[I, O](id, label, isManaged, Duration.Undefined) {
      override def apply(inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
        executor: ExecutionContext
      ): (ColoredMarking, Context, I) => Future[(ColoredMarking, O)] =
        (marking, state, input) => {
          val tokens = outAdjacent.map { case (place, weight) =>
            produceTokens(place, weight.toInt)
          }

          Future.successful(marking -> constant)
        }

      override def updateState(e: O): (Context) => Context = i => i

      override def produceTokens[C](place: Place[C], count: Int): MultiSet[C] =
        MultiSet.empty[C] + (constant.asInstanceOf[C] -> count)
    }

  def nullTransition(id: Long, label: String, isManaged: Boolean = false) =
    constantTransition[Null, Null](id, label, isManaged, null)

  def process[S](params: Arc*)(implicit ec: ExecutionContext): ColoredPetriNetProcess[S] =
    new ScalaGraphPetriNet(Graph(params: _*)) with ColoredTokenGame with TransitionExecutor[S] {
      override val executionContext = ec
    }
}
