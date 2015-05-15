package io.process.statebox.process

import akka.persistence.PersistentActor

object StateBox {

  sealed trait Command

  case class Clone(id: Long) extends Command
  case class CreateInstance(id: Long) extends Command

  sealed trait Event

  case class InstanceCreated(id:Long) extends Event
}


class StateBox extends PersistentActor {

  override def persistenceId: String = ???

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {

    case
  }
}
