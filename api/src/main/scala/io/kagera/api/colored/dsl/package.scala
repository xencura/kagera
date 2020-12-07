package io.kagera.api.colored

import cats.ApplicativeError
import cats.effect.Sync
import io.kagera.api._
import io.kagera.api.colored.transitions.{ AbstractTransition, IdentityTransition }
import io.kagera.api.multiset._
import scalax.collection.Graph
import scalax.collection.edge.WLDiEdge

import scala.concurrent.duration.Duration

/**
 * TODO:
 *
 * This is not much of a DSL yet.
 *
 * Insight:
 *
 * Since each transition is different in what kind of in/out places & edges it can take we should probably not create a
 * general connectivity DSL based on the base trait Transition.
 */
package object dsl {

  implicit class TransitionDSL[Input, Output, State](t: Transition[Input, Output, State]) {
    def ~>(p: Place[_], weight: Long = 1): Arc = arc(t, p, weight)
  }

  implicit class PlaceDSL[C](p: Place[C]) {
    def ~>(t: Transition[_, _, _], weight: Long = 1, filter: C => Boolean = token => true): Arc =
      arc(p, t, weight, filter)
  }

  def arc(t: Transition[_, _, _], p: Place[_], weight: Long): Arc =
    WLDiEdge[Node, String](t, p)(weight, "")

  def arc[C](p: Place[C], t: Transition[_, _, _], weight: Long, filter: C => Boolean = (token: C) => true): Arc = {
    val innerEdge = new PTEdgeImpl[C](weight, filter)
    WLDiEdge[Node, PTEdge[C]](p, t)(weight, innerEdge)
  }

  def constantTransition[I, O, S](id: Long, label: Option[String] = None, automated: Boolean = false, constant: O) =
    new AbstractTransition[I, O, S](id, label.getOrElse(s"t$id"), automated, Duration.Undefined)
      with IdentityTransition[I, O, S] {

      override val toString = label

      override def apply[F[_]](inAdjacent: MultiSet[Place[_]], outAdjacent: MultiSet[Place[_]])(implicit
        sync: Sync[F],
        applicativeError: ApplicativeError[F, Throwable]
      ) =
        (marking, state, input) =>
          sync.delay {
            val produced = outAdjacent.map { case (place, weight) =>
              place -> Map(constant -> weight)
            }.toMarking

            (produced, constant)
          }
    }

  def nullTransition[S](id: Long, label: Option[String] = None, automated: Boolean = false): Transition[Unit, Unit, S] =
    constantTransition[Unit, Unit, S](id, label, automated, ())

  def process[S](params: Arc*): ExecutablePetriNet[S] = {
    val petriNet = new ScalaGraphPetriNet(Graph(params: _*)) with ColoredTokenGame

    requireUniqueElements(petriNet.places.toSeq.map(_.id), "Place identifier")
    requireUniqueElements(petriNet.transitions.toSeq.map(_.id), "Transition identifier")

    petriNet
  }
}
