package io.kagera.api

import scala.concurrent.Future
import scalax.collection.edge.WLDiEdge
import scalaz.syntax.std.boolean._

package object simple {

  implicit def MarkingLike[P]: MarkingLike[Marking[P], P] = new MarkingLike[Marking[P], P] {
    override def emptyMarking: Marking[P] = Map.empty

    override def consume(from: Marking[P], other: Marking[P]): Marking[P] =
      other.foldLeft(from) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => throw new IllegalStateException(s"No such place in marking: $p")
          case Some(n) if n < amount => throw new IllegalStateException(s"Too few tokens in place: $p")
          case Some(n) if n == amount => m - p
          case Some(n) => m + (p -> (n - amount))
        }
      }

    override def produce(into: Marking[P], other: Marking[P]): Marking[P] =
      other.foldLeft(into) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => m + (p -> amount)
          case Some(count) => m + (p -> (count + amount))
        }
      }

    override def multiplicity(marking: Marking[P]): Marking[P] = marking

    override def isSubMarking(marking: Marking[P], other: Marking[P]): Boolean =
      !other.exists { case (place, count) =>
        marking.get(place) match {
          case None => true
          case Some(n) if n <= count => true
          case _ => false
        }
      }
  }

  trait SimpleTokenGame[P, T] extends TokenGame[P, T, Marking[P]] {
    this: PetriNet[P, T] =>

    import ScalaGraph._

    override def consumableMarkings(m: Marking[P])(t: T): Iterable[Marking[P]] = {
      // for uncolored markings there is only 1 consumable marking per transition
      val in = inMarking(t)

      m.isSubMarking(in).option(in)
    }

    lazy val constructors = innerGraph.nodes.collect({
        case node if node.isNodeB && node.incoming.isEmpty => node.valueB
      }: PartialFunction[BiPartiteGraph[P, T, WLDiEdge]#NodeT, T]
    ) // TODO This should not be needed, why does the compiler complain?

    override def enabledTransitions(marking: Marking[P]): Set[T] = {
      marking
        .map { case (place, count) =>
          innerGraph.get(place).outgoing.collect {
            case edge if (edge.weight <= count) => edge.target
          }
        }
        .reduceOption(_ ++ _)
        .getOrElse(Set.empty)
        .collect {
          case node if node.incomingA.subsetOf(marking.keySet) => node.valueB
        } ++ constructors
    }
  }

  trait SimpleExecutor[P, T] extends TransitionExecutor[P, T, Marking[P]] {

    this: PetriNet[P, T] with TokenGame[P, T, Marking[P]] =>

    override def fireTransition(m: Marking[P])(t: T): Future[Marking[P]] =
      Future.successful(m.consume(inMarking(t)).produce(outMarking(t)))
  }
}
