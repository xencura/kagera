package io.kagera.api

import io.kagera.api.multiset.MultiSet
import shapeless.tag
import shapeless.tag._

import scala.language.existentials
import scalax.collection.edge.WLDiEdge

package object colored {

  /**
   * Type alias for the node type of the scalax.collection.Graph backing the petri net.
   */
  type Node = Either[Place[_], Transition[_, _, _]]

  /**
   * Type alias for the edge type of the scalax.collection.Graph backing the petri net.
   */
  type Arc = WLDiEdge[Node]

  type MarkedPlace[T] = (Place[T], MultiSet[T])

  implicit def toMarkedPlace(tuple: (Place[Unit], Int)): MarkedPlace[Unit] = tuple._1 -> Map[Unit, Int](() -> tuple._2)

  implicit class IterableToMarking(i: Iterable[(Place[_], MultiSet[_])]) {
    def toMarking: Marking = Marking(i.toMap[Place[_], MultiSet[_]])
  }

  implicit class ColoredPetriNetAdditions(petriNet: PetriNet[Place[_], Transition[_, _, _]]) {
    def getEdge(p: Place[_], t: Transition[_, _, _]): Option[PTEdge[Any]] =
      petriNet.innerGraph.findPTEdge(p, t).map(_.label.asInstanceOf[PTEdge[Any]])
  }

  implicit def toMarking(map: Map[Place[_], MultiSet[_]]): Marking = Marking(map)

  implicit def placeLabel[C](p: Place[C]): String @@ tags.Label = tag[tags.Label](p.label)

  implicit def placeIdentifier(p: Place[_]): Long @@ tags.Id = tag[tags.Id](p.id)

  implicit def transitionLabeler(t: Transition[_, _, _]): String @@ tags.Label = tag[tags.Label](t.label)

  implicit def transitionIdentifier(t: Transition[_, _, _]): Long @@ tags.Id = tag[tags.Id](t.id)

  type ExceptionHandler = Throwable => ExceptionStrategy

  /**
   * TODO
   *
   * Incorporate the upper bounds of the
   *
   * place color: C transition input: I transition output: E
   *
   * Then that we can define:
   *
   * Uncolored petri nets, where C is Unit Non interactive nets, Where I is Unit
   *
   * @tparam S
   */
  type ExecutablePetriNet[S] = PetriNet[Place[_], Transition[_, _, _]] with ColoredTokenGame with TransitionExecutor[S]
}
