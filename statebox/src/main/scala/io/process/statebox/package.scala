package io.process

import akka.stream.scaladsl.Flow
import io.process.statebox.process.PetriNetActor.TransitionFired
import io.process.statebox.process.StateBox.Command
import io.process.statebox.{ Id, Identifiable, Marking }

package object statebox {

  type Id = Long
  type ProcessModel = String
  type Marking = Map[Id, Set[Int]]
  type Token = (Id, Any)
  type Transition = Long
  type Place = Long
  trait Identifiable { val id: Long }

  type ManagedProcess[M] = Flow[Command, TransitionFired, M]
}

trait PetriNet[P, T] {

  type Marking <: Map[P, Int]

  type Transition = T with Identifiable
  type Place = P with Identifiable
  type EdgeTP = Int
  type EdgePT = Int

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
