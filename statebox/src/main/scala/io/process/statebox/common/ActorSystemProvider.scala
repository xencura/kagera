package io.process.statebox.common

import akka.actor.ActorSystem

trait ActorSystemProvider {

  def system: ActorSystem
}
