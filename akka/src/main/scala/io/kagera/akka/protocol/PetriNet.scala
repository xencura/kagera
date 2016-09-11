package io.kagera.akka.protocol

object PetriNet {

  trait TransitionState

  case object Enabled
  case class Disabled(reason: String)

  trait Response

  case object PartialCausedFirst
  case object PartialCausedN
  case object CausalFirst
  case object CausalN
  case object Any
}
