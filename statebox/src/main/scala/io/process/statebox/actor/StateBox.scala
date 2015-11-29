package io.process.statebox.actor

import akka.actor.Actor
import io.process.statebox.actor.StateBox.CreateInstance
import io.process.statebox.process.colored.ColoredPetriNet

object StateBox {

  sealed trait Command

  case class Clone(id: Long) extends Command
  case class CreateInstance(process: ColoredPetriNet) extends Command
  case object GetIndex extends Command

  sealed trait Event

  case class InstanceCreated(id: Long) extends Event
}

class StateBox extends Actor {
  override def receive: Receive = {
    case CreateInstance(process) => {
      context.actorOf(PetriNetActor.props(process))
    }
  }
}
