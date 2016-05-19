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
      val newTokens =
        marking(place).filterNot(tokensToRemove.contains) // TODO BUG: Will consume all duplicate tokens (not subset)
      if (newTokens.isEmpty)
        marking - place
      else
        marking + (place -> newTokens)
    }

    override def produce(into: ColoredMarking, other: ColoredMarking): ColoredMarking = other.foldLeft(into) {
      case (marking, (place, tokens)) => marking + (place -> tokens.++(marking.getOrElse(place, Seq.empty)))
    }

    override def isSubMarking(marking: ColoredMarking, other: ColoredMarking): Boolean = other.forall {
      case (place, tokens) => marking.get(place).exists(markingTokens => tokens.forall(markingTokens.contains))
    }
  }

  trait ColoredTokenGame extends TokenGame[Place, Transition, ColoredMarking] {
    this: PetriNet[Place, Transition] =>

    override def enabledParameters(m: ColoredMarking): Map[Transition, Iterable[ColoredMarking]] =
      enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap

    def consumableMarkings(marking: ColoredMarking)(t: Transition): Iterable[ColoredMarking] = {
      val firstEnabled = inMarking(t).map { case (place, count) =>
        place -> marking(place).take(count.toInt)
      }
      Seq(firstEnabled)
    }

    override def enabledTransitions(marking: ColoredMarking): Set[Transition] =
      simple.findEnabledTransitions(this)(marking.multiplicity)
  }

  def executeTransition(
    pn: PetriNet[Place, Transition]
  )(consume: ColoredMarking, t: Transition, extraData: Option[Any])(implicit
    ec: ExecutionContext
  ): Future[ColoredMarking] = {

    try {

      val inAdjacent = consume.map { case (place, data) =>
        (place, pn.innerGraph.connectingEdgeAB(place, t), data)
      }.toSeq

      val outAdjacent = pn
        .outAdjacentPlaces(t)
        .map { case place =>
          (pn.innerGraph.connectingEdgeBA(t, place), place)
        }
        .toSeq

      val input = t.createInput(inAdjacent, extraData)

      t.apply(input).map(t.createOutput(_, outAdjacent))

    } catch {
      case e: Exception => Future.failed(e)
    }
  }

  trait ColoredExecutor extends TransitionExecutor[Place, Transition, ColoredMarking] {

    this: PetriNet[Place, Transition] with TokenGame[Place, Transition, ColoredMarking] =>

    override def fireTransition(
      marking: ColoredMarking
    )(t: Transition, data: Option[Any])(implicit ec: ExecutionContext) = {

      // pick the tokens
      enabledParameters(marking)
        .get(t)
        .flatMap(_.headOption)
        .map { consume =>
          executeTransition(this)(consume, t, data)
            .recoverWith { case e: Exception =>
              Future.failed(new RuntimeException(s"Transition '$t' failed to fire!", e))
            }
            .map(produce => marking.consume(consume).produce(produce))

        }
        .getOrElse { throw new IllegalStateException(s"Transition $t is not enabled") }
    }
  }

  trait ColoredPetriNetProcess
      extends PetriNetProcess[Place, Transition, ColoredMarking]
      with ColoredTokenGame
      with ColoredExecutor
}
