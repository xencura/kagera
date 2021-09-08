package io.kagera.api.colored

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

  implicit class TransitionDSL[T <: Transition[_, _, _]](t: T) {
    def ~>(p: Place[_], weight: Long = 1): Arc[T] = arc(t, p, weight)
  }

  implicit class PlaceDSL[C](p: Place[C]) {
    def ~>[T](t: T, weight: Long = 1, filter: C => Boolean = token => true): Arc[T] = arc(p, t, weight, filter)
  }

  def arc[T](t: T, p: Place[_], weight: Long): Arc[T] = WLDiEdge[Node[T], String](Right(t), Left(p))(weight, "")

  def arc[C, T](p: Place[C], t: T, weight: Long, filter: C => Boolean = (token: C) => true): Arc[T] = {
    val innerEdge = new PTEdgeImpl[C](weight, filter)
    WLDiEdge[Node[T], PTEdge[C]](Left(p), Right(t))(weight, innerEdge)
  }

  class ConstantTransition[I, O, S](id: Long, label: String, automated: Boolean, val constant: O)
      extends AbstractTransition[I, O, S](id, label, automated, Duration.Undefined)
      with IdentityTransition[I, O, S] {

    override val toString = label
  }
  def constantTransition[I, O, S](id: Long, label: Option[String] = None, automated: Boolean = false, constant: O) =
    new ConstantTransition[I, O, S](id, label.getOrElse(s"t$id"), automated, constant)

  class ConstantTransitionExecutorFactory[F[_] : Sync, I, O, S]
      extends TransitionExecutorFactory[F, ConstantTransition[I, O, S]] {
    type Input = I
    type Output = O
    type State = S

    override def createTransitionExecutor(
      t: ConstantTransition[I, O, S],
      inAdjacent: MultiSet[Place[_]],
      outAdjacent: MultiSet[Place[_]]
    ): TransitionFunctionF[F, I, O, S] =
      (marking, state, input) =>
        Sync.apply.delay {
          val produced = outAdjacent.map { case (place, weight) =>
            place -> Map(t.constant -> weight)
          }.toMarking

          (produced, t.constant)
        }
  }

  def nullTransition[S](
    id: Long,
    label: Option[String] = None,
    automated: Boolean = false
  ): ConstantTransition[Unit, Unit, S] =
    constantTransition[Unit, Unit, S](id, label, automated, ())

  def process[S, T <: Transition[_, _, _]](params: Arc[T]*): ExecutablePetriNet[S, T] = {
    val petriNet = new ScalaGraphPetriNet[Place[_], T](Graph(params: _*)) with ColoredTokenGame[T]

    requireUniqueElements(petriNet.places.toSeq.map(_.id), "Place identifier")
    requireUniqueElements(petriNet.transitions.toSeq.map(_.id), "Transition identifier")

    petriNet
  }
}
