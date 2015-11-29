package io.process.statebox

import akka.stream.scaladsl.Flow
import io.process.statebox.actor.PetriNetActor.TransitionFired
import io.process.statebox.actor.StateBox.Command
import io.process.statebox.actor.{ PetriNetActor, StateBox }

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

package object process {

  type Id = Long

  type ManagedProcess[M] = Flow[Command, TransitionFired, M]

  trait Identifiable {
    def id: Long
  }

  trait HasLabel {
    def label: String
  }

  trait MarkingLike[M, P] {

    def tokens(marking: M): Map[P, Int]

    def consume(from: M, other: M): M

    def produce(into: M, other: M): M
  }

  trait PetriNet[P, T] {

    type Node = Either[P, T]

    def places: Set[P]
    def transitions: Set[T]

    def inMarking(t: T): Map[P, Int]
    def outMarking(t: T): Map[P, Int]

    def nodes: Set[Node]
  }

  trait TransitionExecutor[P, T, M] {

    this: PetriNet[P, T] =>

    def fireTransition(marking: M)(transition: T, consume: M): M
  }

  trait TokenGame[M, P, T] {

    this: PetriNet[P, T] =>

    type Marking
    implicit def m: MarkingLike[Marking, P]

    def consumableMarkings(m: Marking)(t: T): Iterable[Marking]

    def fireableTransitions(m: Marking): Iterable[T] = transitions.filter(t => isEnabled(m)(t))
    def isEnabled(m: Marking)(t: T) = consumableMarkings(m)(t).nonEmpty
  }

  /// -----

  trait ScalaGraphWrapper[P, T] extends PetriNet[P, T] {

    val innerGraph: Graph[Node, WDiEdge]

  }

}
