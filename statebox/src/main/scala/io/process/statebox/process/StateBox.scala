package io.process.statebox.process

object StateBox {

  sealed trait Command

  case class Clone(id: Long) extends Command
  case class CreateInstance(id: Long) extends Command

  sealed trait Event

  case class InstanceCreated(id: Long) extends Event
}
