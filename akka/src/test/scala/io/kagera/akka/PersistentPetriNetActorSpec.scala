package io.kagera.akka

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Props, Terminated }
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import io.kagera.akka.actor.PersistentPetriNetActor
import io.kagera.akka.actor.PersistentPetriNetActor.{
  FireTransition,
  GetState,
  State,
  TransitionFailed,
  TransitionFiredSuccessfully
}
import io.kagera.api.colored._
import io.kagera.api.colored.dsl._
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
      |logging.root.level = WARN
    """.stripMargin)
}

class PersistentPetriNetActorSpec
    extends TestKit(ActorSystem("test", PersistentPetriNetActorSpec.config))
    with WordSpecLike
    with ImplicitSender {

  trait Event
  case class Added(n: Int) extends Event
  case class Removed(n: Int) extends Event

  val eventSourcing: Set[Int] => Event => Set[Int] = set => {
    case Added(c) => set + c
    case Removed(c) => set - c
  }

  val p1 = Place[Unit](id = 1, label = "p1")
  val p2 = Place[Unit](id = 2, label = "p2")
  val p3 = Place[Unit](id = 3, label = "p3")

  import system.dispatcher

  "A persistent petri net actor" should {

    "Respond with a TransitionFiredSuccessfully message if a transition fired successfully" in {}

    "Respond with a TransitionFailed message if a transition failed to fire" in {

      val t1 = stateFunction(eventSourcing)(set => throw new RuntimeException("something went wrong"))

      val petriNet = process[Set[Int]](p1 ~> t1, t1 ~> p2)

      val id = UUID.randomUUID()
      val initialMarking = ColoredMarking(p1 -> 1)

      val actor = system.actorOf(Props(new PersistentPetriNetActor[Set[Int]](id, petriNet, initialMarking, Set.empty)))

      actor ! FireTransition(t1, ())

      expectMsgClass(classOf[TransitionFailed[_]])
    }

    "Be able to restore it's state after termination" in {

      val t1 = stateFunction(eventSourcing)(set => Added(1))
      val t2 = stateFunction(eventSourcing, isManaged = true)(set => Added(2))

      val petriNet = process[Set[Int]](p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)

      // creates a petri net actor with initial marking: p1 -> 1
      val id = UUID.randomUUID()
      val initialMarking = ColoredMarking(p1 -> 1)

      val actor = system.actorOf(Props(new PersistentPetriNetActor[Set[Int]](id, petriNet, initialMarking, Set.empty)))

      // assert that the actor is in the initial state
      actor ! GetState

      expectMsg(State[Set[Int]](initialMarking, Set.empty))

      // fire the first transition (t1) manually
      actor ! FireTransition(t1, ())

      // expect the next marking: p2 -> 1
      expectMsgPF() { case TransitionFiredSuccessfully(t1, _, _, result, _) if result == ColoredMarking(p2 -> 1) => }

      // since t2 fires automatically we also expect the next marking: p3 -> 1
      expectMsgPF() { case TransitionFiredSuccessfully(t2, _, _, result, _) if result == ColoredMarking(p3 -> 1) => }

      // terminate the actor
      watch(actor)
      actor ! PoisonPill
      expectMsgClass(classOf[Terminated])

      // create a new actor with the same persistent identifier
      val newActor =
        system.actorOf(Props(new PersistentPetriNetActor[Set[Int]](id, petriNet, initialMarking, Set.empty)))

      newActor ! GetState

      // assert that the marking is the same as before termination
      expectMsg(State[Set[Int]](ColoredMarking(p3 -> 1), Set(1, 2)))
    }
  }
}
