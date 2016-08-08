package io.kagera.api

import io.kagera.api.multiset._
import io.kagera.api.tags.Label

import scala.language.existentials
import scalax.collection.edge.WLDiEdge
import scalaz.{ @@, Tag }

package object colored {

  /**
   * Type alias for the node type of the scalax.collection.Graph backing the petri net.
   */
  type Node = Either[Place[_], Transition]

  /**
   * Type alias for the edge type of the scalax.collection.Graph backing the petri net.
   */
  type Arc = WLDiEdge[Node]

  def placeLabel[C](p: Place[C]): @@[String, Label] = Tag[String, Label](p.label)

  implicit object TransitionLabeler extends Labeled[Transition] {
    override def apply(t: Transition): @@[String, Label] = Tag[String, Label](t.label)
  }

  type ColoredPetriNetProcess[S] = PetriNet[Place[_], Transition] with ColoredTokenGame with TransitionExecutor[S]
}
