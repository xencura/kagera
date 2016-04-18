package io.kagera.akka.actor

import akka.actor.ActorRef

object PetriNetDebugging {

  sealed trait DebugCommand

  case class SetBreakPoint(t: Transition, receiver: ActorRef) extends DebugCommand
  case object Step extends DebugCommand
  case object Resume extends DebugCommand
  case class RemoveBreakPoint(t: Transition) extends DebugCommand
}
