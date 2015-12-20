package io.process.statebox.process

import org.slf4j.LoggerFactory

package object simple {

  implicit class MarkingFunctions[P](marking: Marking[P]) {
    def consume(other: Marking[P]): Marking[P] = {

      other.foldLeft(marking) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => throw new IllegalStateException(s"No such place in marking: $p")
          case Some(n) if n < amount => throw new IllegalStateException(s"Too few tokens in place: $p")
          case Some(n) if n == amount => m - p
          case Some(n) => m + (p -> (n - amount))
        }
      }
    }

    def produce(other: Marking[P]): Marking[P] = {
      other.foldLeft(marking) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => m + (p -> amount)
          case Some(count) => m + (p -> (count + amount))
        }
      }
    }

    def isSubMarking(other: Marking[P]): Boolean = {
      !other.exists { case (place, count) =>
        marking.get(place) match {
          case None => true
          case Some(n) if n < count => true
          case _ => false
        }
      }
    }
  }

  trait SimpleTokenGame[P, T] extends TokenGame[P, T, Marking[P]] {
    this: PetriNet[P, T] =>

    import ScalaGraph._

    override def consumableMarkings(m: Marking[P])(t: T): Iterable[Marking[P]] = {
      // for uncolored markings there is only 1 consumable marking per transition
      val in = inMarking(t)

      if (m.isSubMarking(in))
        List(in)
      else
        List.empty
    }

    lazy val constructors = innerGraph.nodes.collect({
        case node if node.isNodeB && node.incoming.isEmpty => node.valueB
      }: PartialFunction[BiPartiteGraph[P, T]#NodeT, T]
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

    this: PetriNet[P, T] =>

    val log = LoggerFactory.getLogger(classOf[SimpleExecutor[_, _]])

    override def fireTransition(marking: Marking[P])(transition: T, consume: Marking[P]): Marking[P] = {
      log.debug("Firing transition {}", transition)

      val out = outMarking(transition)
      log.debug("outMarking: {}", out)

      val newMarking = marking.consume(consume).produce(out)

      log.info("fired transition {}, result: {}", transition, newMarking)

      newMarking
    }
  }
}
