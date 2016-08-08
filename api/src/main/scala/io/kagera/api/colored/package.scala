package io.kagera.api

import io.kagera.api.tags.Label

import scala.language.existentials
import scalax.collection.edge.WLDiEdge
import scalaz.{ @@, Tag }

package object colored {

  /**
   * Type alias for the node type of the scalax.collection.Graph backing the petri net.
   */
  type Node = Either[Place[_], Transition[_, _, _]]

  /**
   * Type alias for the edge type of the scalax.collection.Graph backing the petri net.
   */
  type Arc = WLDiEdge[Node]

  implicit def placeLabel[C](p: Place[C]): @@[String, Label] = Tag[String, Label](p.label)

  implicit def placeIdentifier(p: Place[_]): @@[Long, tags.Id] = Tag[Long, tags.Id](p.id)

  implicit object TransitionLabeler extends Labeled[Transition[_, _, _]] {
    override def apply(t: Transition[_, _, _]): @@[String, tags.Label] = Tag[String, tags.Label](t.label)
  }

  implicit object TransitionIdentifier extends Identifiable[Transition[_, _, _]] {
    override def apply(t: Transition[_, _, _]): @@[Long, tags.Id] = Tag[Long, tags.Id](t.id)
  }

  type ColoredPetriNetProcess[S] = PetriNet[Place[_], Transition[_, _, _]]
    with ColoredTokenGame
    with TransitionExecutor[S]
}
