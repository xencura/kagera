package io.process.statebox.process

import akka.actor.ActorRef
import io.process.statebox.process.dsl.Transition

object PetriNetDebugging {

  sealed trait DebugCommand

  case class SetBreakPoint(t: Transition, receiver: ActorRef) extends DebugCommand
  case object Step extends DebugCommand
  case object Resume extends DebugCommand
  case class RemoveBreakPoint(t: Transition) extends DebugCommand
}
