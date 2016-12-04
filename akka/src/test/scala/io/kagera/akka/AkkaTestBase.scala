package io.kagera.akka

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.{ BeforeAndAfterAll, Suite, WordSpecLike }

abstract class AkkaTestBase
    extends TestKit(ActorSystem("testSystem", PetriNetInstanceSpec.config))
    with WordSpecLike
    with ImplicitSender
    with BeforeAndAfterAll {

  override def afterAll() = {
    super.afterAll()
    shutdown(system)
  }
}
