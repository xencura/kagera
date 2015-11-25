package io.process

import akka.stream.scaladsl.Flow
import io.process.statebox.process.PetriNetActor.TransitionFired
import io.process.statebox.process.StateBox.Command
import io.process.statebox.process.dsl.Place

package object statebox {

  type Id = Long
  type Token = (Id, Any)

  type ManagedProcess[M] = Flow[Command, TransitionFired, M]

  type SimpleMarking[P] = Map[P, Long]

  implicit class MarkingFunctions[P](marking: SimpleMarking[P]) {
    def consume(other: SimpleMarking[P]) = {
      other.foldLeft(marking) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => throw new IllegalStateException(s"No such place in marking: $p")
          case Some(count) if count < amount => throw new IllegalStateException(s"Too few tokens in place: $p")
          case Some(count) if count == amount => m - p
          case Some(count) => m + (p -> (count - amount))
        }
      }
    }

    def produce(other: SimpleMarking[P]) = {
      other.foldLeft(marking) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => m + (p -> amount)
          case Some(count) => m + (p -> (count + amount))
        }
      }
    }
  }

  trait PetriNet[P, T] {

    type Marking <: Map[P, Int]

    def places: Set[P]
    def transitions: Set[T]
  }

  trait PTProcess[T, P] extends PetriNet[P, T] with TokenGame[P, T] with TransitionExecutor[P, T] {}

  trait TransitionExecutor[P, T] {

    self: PetriNet[P, T] =>

    def fire(transition: T, consume: Marking): Marking
  }

  trait TokenGame[P, T] {

    self: PetriNet[P, T] =>

    def fireable(m: Marking): Iterable[T]
    def consumableMarkings(m: Marking)(t: T): Iterable[Marking]
    def enabledTransitions(m: Marking): Iterable[T] = transitions.filter(t => isEnabled(m)(t))
    def isEnabled(m: Marking)(t: T) = consumableMarkings(m)(t).nonEmpty
  }
}
