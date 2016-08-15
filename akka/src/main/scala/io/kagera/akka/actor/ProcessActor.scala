package io.kagera.akka.actor

import akka.persistence.PersistentActor
import io.kagera.api.colored.ColoredMarking

object ProcessActor {

  case class CreateProcess()

  case class Clone()

  case class ProcessCreated(id: java.util.UUID, initialMarking: ColoredMarking)
}

class ProcessActor extends PersistentActor {

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = ???

  override def persistenceId: String = ???
}
