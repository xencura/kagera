package io.process.statebox.common

import akka.actor.ActorSystem

trait ActorSystemProvider {

  implicit val system: ActorSystem
}
