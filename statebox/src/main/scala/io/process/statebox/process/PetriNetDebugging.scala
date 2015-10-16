package io.process.statebox.process

import akka.actor.ActorRef
import io.process.statebox.Transition

object PetriNetDebugging {
  sealed trait Command

  case class SetBreakPoint(t: Transition, receiver: ActorRef) extends Command
  case object Step extends Command
  case object Resume extends Command
  case class RemoveBreakPoint(t: Transition) extends Command
}

trait PetriNetDebugging[T, P] {

  self: PetriNetActor[T, P] =>

  val breakPoints: Map[Transition, ActorRef]

  // override receive command
  override def receiveCommand: Unit = {}
}
