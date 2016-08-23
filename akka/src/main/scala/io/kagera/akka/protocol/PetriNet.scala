package io.kagera.akka.protocol

object PetriNet {

  trait TransitionState

  case object Enabled
  case class Disabled(reason: String)
}
