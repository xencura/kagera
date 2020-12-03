package io.kagera.api

import io.kagera.api.multiset._
import scala.collection.compat._
import scalax.collection.edge.WLDiEdge

import scala.language.{ existentials, higherKinds }

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
  type TransitionFunction[F[_], Input, Output, State] = (Marking, State, Input) => F[(Marking, Output)]

  /**
   * An exception handler function associated with a transition.
   */
  type TransitionExceptionHandler = (Throwable, Int) => ExceptionStrategy

  /**
   * Type alias for a colored petri net.
   */
  type ColoredPetriNet = PetriNet[Place[_], Transition[_, _, _]]

  /**
   * Type alias for a marking.
   */
  type Marking = HMap[Place, MultiSet]

  /**
   * Some convenience method additions to work with Markings.
   */
  implicit class MarkingAdditions(marking: Marking) {

    def multiplicities: MultiSet[Place[_]] = marking.data.view.mapValues(_.multisetSize).toMap

    def add[C](place: Place[C], value: C, count: Int = 1): Marking =
      marking.updatedWith(place)(_.map(_.multisetIncrement(value, count)).orElse(Some(MultiSet(value))))
    def remove[C](place: Place[C], value: C, count: Int = 1): Marking =
      marking.updatedWith(place)(_.map(_.multisetDecrement(value, count)))
    def replace[T](fromPlace: Place[T], toPlace: Place[T], fromToken: T, toToken: T): Marking =
      marking.remove(fromPlace, fromToken).add(toPlace, toToken)
    def move[T](from: Place[T], to: Place[T], token: T): Marking = replace(from, to, token, token)
    def updateIn[T](place: Place[T], from: T, to: T): Marking = replace(place, place, from, to)

    def |-|(other: Marking): Marking = other.keySet.foldLeft(marking) { case (result, place) =>
      marking.get(place) match {
        case None => result
        case Some(tokens) =>
          val newTokens = tokens.multisetDifference(other(place))
          if (newTokens.isEmpty)
            result - place
          else
            result + (place -> newTokens)
      }
    }

    def |+|(other: Marking): Marking = other.keySet.foldLeft(marking) { case (result, place) =>
      val newTokens = marking.get(place) match {
        case None => other(place)
        case Some(tokens) => tokens.multisetSum(other(place))
      }

      result + (place -> newTokens)
    }
  }

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
    def toMarking: Marking = HMap[Place, MultiSet](i.toMap[Place[_], MultiSet[_]])
  }

  implicit class ColoredPetriNetAdditions(petriNet: ColoredPetriNet) {
    def getEdge(p: Place[_], t: Transition[_, _, _]): Option[PTEdge[Any]] =
      petriNet.innerGraph.findPTEdge(p, t).map(_.label.asInstanceOf[PTEdge[Any]])
  }

  implicit def toMarking(map: Map[Place[_], MultiSet[_]]): Marking = HMap[Place, MultiSet](map)

  implicit def placeLabel[C](p: Place[C]): Label = Label(p.label)

  implicit def placeIdentifier(p: Place[_]): Id = Id(p.id)

  implicit def transitionLabeler(t: Transition[_, _, _]): Label = Label(t.label)

  implicit def transitionIdentifier(t: Transition[_, _, _]): Id = Id(t.id)
}
