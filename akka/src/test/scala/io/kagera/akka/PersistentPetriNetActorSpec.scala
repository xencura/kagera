package io.kagera.akka

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Props, Terminated }
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.{ Config, ConfigFactory }
import io.kagera.akka.actor.PersistentPetriNetActor
import io.kagera.akka.actor.PersistentPetriNetActor.{ FireTransition, GetState, TransitionFired }
import io.kagera.api.colored.dsl._
import io.kagera.api.colored._
import org.scalatest.WordSpecLike

object PersistentPetriNetActorSpec {
  val config = ConfigFactory.parseString("""
      |akka {
      |  loggers = ["akka.testkit.TestEventListener"]
      |
      |  persistence.journal.plugin = "akka.persistence.journal.inmem"
      |  persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |  actor.provider = "akka.actor.LocalActorRefProvider"
      |}
      |
      |cram.list-authorized-parties.endpoint-uri = "direct:mdmEndpoint"
      |
      |logging.root.level = WARN
    """.stripMargin)
}

class PersistentPetriNetActorSpec
    extends TestKit(ActorSystem("test", PersistentPetriNetActorSpec.config))
    with WordSpecLike
    with ImplicitSender {

  val p1 = Place[Unit](id = 1, label = "p1")
  val p2 = Place[Unit](id = 2, label = "p2")
  val p3 = Place[Unit](id = 3, label = "p3")

  val t1 = nullTransition(1, "t1", isManaged = false)
  val t2 = nullTransition(2, "t2", isManaged = true)

  import system.dispatcher

  val petriNet = process[Unit](p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)

  "A persistent petri net actor" should {

    "Be able to restore it's state after termination" in {

      // creates a petri net actor with initial marking: p1 -> 1
      val id = UUID.randomUUID()
      val initialMarking = ColoredMarking(p1(()))

      val actor = system.actorOf(Props(new PersistentPetriNetActor[Unit](id, petriNet, initialMarking, ())))

      // assert that the actor is in the initial state
      actor ! GetState

      expectMsg(initialMarking)

      // fire the first transition (t1) manually
      actor ! FireTransition(t1, ())

      // expect the next marking: p2 -> 1
      expectMsg(ColoredMarking(p2(())))

      // since t2 fires automatically we also expect the next marking: p3 -> 1
      expectMsg(ColoredMarking(p3(())))

      // terminate the actor
      watch(actor)
      actor ! PoisonPill
      expectMsgClass(classOf[Terminated])

      // create a new actor with the same persistent identifier
      val newActor = system.actorOf(Props(new PersistentPetriNetActor[Unit](id, petriNet, initialMarking, ())))

      newActor ! GetState

      // assert that the marking is the same as before termination
      expectMsg(ColoredMarking(p3(())))
    }
  }

}
