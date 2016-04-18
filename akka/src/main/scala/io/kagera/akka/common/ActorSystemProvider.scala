package io.kagera.akka.common

import akka.actor.ActorSystem

trait ActorSystemProvider {

  implicit val system: ActorSystem
}
