package io.kagera.api

import fs2.Task
import io.kagera.api.multiset.MultiSet

import scala.language.existentials
import scalax.collection.Graph
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

  /**
   * Type alias for a single marked place, meaning a place containing tokens.
   *
   * @tparam Color
   *   the color of the place.
   */
  type MarkedPlace[Color] = (Place[Color], MultiSet[Color])

  /**
   * An (asynchronous) function associated with a transition
   *
   * @tparam Input
   *   The input delivered to the transition from outside the process.
   * @tparam Output
   *   The output emitted by the transition.
   * @tparam State
   *   The state the transition closes over.
   */
  type TransitionFunction[Input, Output, State] = (Marking, State, Input) => Task[(Marking, Output)]

  /**
   * An exception handler function associated with a transition.
   */
  type TransitionExceptionHandler = (Throwable, Int) => ExceptionStrategy

  /**
   * Type alias for a colored petri net.
   */
  type ColoredPetriNet = PetriNet[Place[_], Transition[_, _, _]]

  /**
   * TODO
   *
   * Incorporate the upper bounds of type parameters into this type
   *
   * Place color: C Transition input: I Transition output: E
   *
   * That way we can define different types of Petri nets:
   *
   * Uncolored petri nets, where C is Unit Non interactive nets, Where I is Unit Nets without State & Event sourcing,
   * Where S & E are Unit
   *
   * @tparam S
   *   The 'global' state transitions close over
   */
  type ExecutablePetriNet[S] = ColoredPetriNet with ColoredTokenGame

  implicit def toMarkedPlace(tuple: (Place[Unit], Int)): MarkedPlace[Unit] = tuple._1 -> Map[Unit, Int](() -> tuple._2)

  implicit class IterableToMarking(i: Iterable[(Place[_], MultiSet[_])]) {
    def toMarking: Marking = Marking(i.toMap[Place[_], MultiSet[_]])
  }

  implicit class ColoredPetriNetAdditions(petriNet: ColoredPetriNet) {
    def getEdge(p: Place[_], t: Transition[_, _, _]): Option[PTEdge[Any]] =
      petriNet.innerGraph.findPTEdge(p, t).map(_.label.asInstanceOf[PTEdge[Any]])
  }

  implicit def toMarking(map: Map[Place[_], MultiSet[_]]): Marking = Marking(map)

  implicit def placeLabel[C](p: Place[C]): Label = Label(p.label)

  implicit def placeIdentifier(p: Place[_]): Id = Id(p.id)

  implicit def transitionLabeler(t: Transition[_, _, _]): Label = Label(t.label)

  implicit def transitionIdentifier(t: Transition[_, _, _]): Id = Id(t.id)
}
