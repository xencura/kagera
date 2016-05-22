package io.kagera.api

import io.kagera.api.ScalaGraph._
import io.kagera.api.tags.Label

import scala.concurrent.{ ExecutionContext, Future }
import scalax.collection.edge.WLDiEdge
import scalaz.{ @@, Tag }

package object colored {

  /**
   * Type alias for the node type of the scalax.collection.Graph backing the petri net.
   */
  type Node = Either[Place, Transition]

  /**
   * Type alias for the edge type of the scalax.collection.Graph backing the petri net.
   */
  type Arc = WLDiEdge[Node]

  /**
   * Type alias for a colored marking, each token in place may hold data.
   */
  type ColoredMarking = Map[Place, Seq[Any]]

  /**
   * Very inefficient multi set operations on Seq, TODO perhaps use https://github.com/nicolasstucki/multisets ? There
   * are some problems with the library though: https://github.com/nicolasstucki/multisets/issues
   */
  implicit class MultiSetSeqOperations[T](seq: Seq[T]) {
    def multiSetRemove(other: Seq[T]) = {
      other.foldLeft(seq) { case (result, element) =>
        result.indexOf(element) match {
          case -1 => result
          case n =>
            result.splitAt(n) match {
              case (first, second) => first ++ second.tail
            }
        }
      }
    }

    def multiSetAdd(other: Seq[T]) = seq ++ other
  }

  implicit object PlaceLabeler extends Labeled[Place] {
    override def apply(p: Place): @@[String, Label] = Tag[String, Label](p.label)
  }

  implicit object TransitionLabeler extends Labeled[Transition] {
    override def apply(t: Transition): @@[String, Label] = Tag[String, Label](t.label)
  }

  implicit object ColoredMarkingLike extends MarkingLike[ColoredMarking, Place] {

    override def emptyMarking: ColoredMarking = Map.empty

    override def multiplicity(marking: ColoredMarking): Marking[Place] = marking.map { case (p, tokens) =>
      (p, tokens.size.toLong)
    }

    override def consume(from: ColoredMarking, other: ColoredMarking): ColoredMarking = other.foldLeft(from) {
      case (marking, (place, tokens)) => removeTokens(marking, place, tokens)
    }

    override def remove(from: ColoredMarking, other: ColoredMarking): ColoredMarking = other.foldLeft(from) {
      case (marking, (place, tokens)) =>
        // Check if tokens exist in from place in other marking.
        val currentTokens = marking.get(place)

        // If tokens are available then remove them out of the list
        currentTokens match {
          case Some(ts) => removeTokens(marking, place, tokens)
          case None => marking - place
        }
    }

    private def removeTokens(marking: ColoredMarking, place: Place, tokensToRemove: Seq[Any]): ColoredMarking = {
      val newTokens = marking(place).multiSetRemove(tokensToRemove)
      if (newTokens.isEmpty) marking - place
      else marking + (place -> newTokens)
    }

    override def produce(into: ColoredMarking, other: ColoredMarking): ColoredMarking = other.foldLeft(into) {
      case (marking, (place, tokens)) => marking + (place -> tokens.++(marking.getOrElse(place, Seq.empty)))
    }

    override def isSubMarking(marking: ColoredMarking, other: ColoredMarking): Boolean = other.forall {
      case (place, tokens) => marking.get(place).exists(markingTokens => tokens.forall(markingTokens.contains))
    }
  }

  trait ColoredPetriNetProcess
      extends PetriNetProcess[Place, Transition, ColoredMarking]
      with ColoredTokenGame
      with ColoredExecutor
}
