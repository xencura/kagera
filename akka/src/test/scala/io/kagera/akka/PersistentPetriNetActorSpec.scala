package io.kagera.akka

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Props, Terminated }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import com.typesafe.config.ConfigFactory
import io.kagera.akka.PersistentPetriNetActorSpec._
import io.kagera.akka.actor.PetriNetProcess
import io.kagera.akka.actor.PetriNetProcess._
import io.kagera.api.colored._
import io.kagera.api.colored.dsl._
import io.kagera.api.colored.transitions.UncoloredTransition
import io.kagera.api.multiset._
import org.scalatest.WordSpecLike
import org.scalatest.time.{ Millisecond, Milliseconds, Span }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration
import scala.util.Random

object PersistentPetriNetActorSpec {

  sealed trait Event
  case class Added(n: Int) extends Event
  case class Removed(n: Int) extends Event

  def stateTransition[S, E](
    eventSourcing: S => E => S,
    fn: S => E,
    id: Long = Random.nextLong,
    label: String = "",
    isManaged: Boolean = false
  ): Transition[Unit, E, S] =
    new AbstractTransition[Unit, E, S](id, label, isManaged, maximumOperationTime = Duration.Undefined)
      with UncoloredTransition[Unit, E, S] {

      override val updateState = eventSourcing

      override def produceEvent(consume: Marking, state: S, input: Unit)(implicit
        executor: ExecutionContext
      ): Future[E] = Future { (fn(state)) }
    }

  val config = ConfigFactory.parseString("""
      |akka {
      |  loggers = ["akka.testkit.TestEventListener"]
      |
      |  persistence.journal.plugin = "akka.persistence.journal.inmem"
      |  persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |  actor.provider = "akka.actor.LocalActorRefProvider"
      |
      |  actor.serializers {
      |    scalapb = "io.kagera.akka.actor.ScalaPBSerializer"
      |  }
      |
      |  actor.serialization-bindings {
      |    "com.trueaccord.scalapb.GeneratedMessage" = scalapb
      |  }
      |}
      |
      |logging.root.level = WARN
    """.stripMargin)
}

class PersistentPetriNetActorSpec
    extends TestKit(ActorSystem("test", PersistentPetriNetActorSpec.config))
    with WordSpecLike
    with ImplicitSender {

  def expectMsgInAnyOrderPF[Out](pfs: PartialFunction[Any, Out]*): Unit = {
    if (pfs.nonEmpty) {
      val total = pfs.reduce((a, b) => a.orElse(b))
      expectMsgPF() {
        case msg @ _ if total.isDefinedAt(msg) =>
          val index = pfs.indexWhere(pf => pf.isDefinedAt(msg))
          val pfn = pfs(index)
          pfn(msg)
          expectMsgInAnyOrderPF[Out](pfs.take(index) ++ pfs.drop(index + 1): _*)
      }
    }
  }

  val eventSourcing: Set[Int] => Event => Set[Int] = set => {
    case Added(c) => set + c
    case Removed(c) => set - c
  }

  val p1 = Place[Unit](id = 1, label = "p1")
  val p2 = Place[Unit](id = 2, label = "p2")
  val p3 = Place[Unit](id = 3, label = "p3")

  import system.dispatcher

  "A persistent petri net actor" should {

    "Respond with a TransitionFailed message if a transition failed to fire" in {

      val t1 = stateTransition[Set[Int], Event](eventSourcing, set => throw new RuntimeException("something went wrong"))

      val petriNet = process[Set[Int]](p1 ~> t1, t1 ~> p2)

      val id = UUID.randomUUID()
      val initialMarking = Marking(p1 -> 1)

      val actor = system.actorOf(Props(new PetriNetProcess[Set[Int]](petriNet, initialMarking, Set.empty)))

      actor ! FireTransition(t1, ())

      expectMsgClass(classOf[TransitionFailed])
    }

    "Be able to restore it's state after termination" in {

      val actorName = java.util.UUID.randomUUID().toString

      val t1 = stateTransition[Set[Int], Event](eventSourcing, set => Added(1))
      val t2 = stateTransition[Set[Int], Event](eventSourcing, set => Added(2), isManaged = true)

      val petriNet = process[Set[Int]](p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)

      // creates a petri net actor with initial marking: p1 -> 1
      val initialMarking = Marking(p1 -> 1)

      val actor = system.actorOf(Props(new PetriNetProcess[Set[Int]](petriNet, initialMarking, Set.empty)), actorName)

      // assert that the actor is in the initial state
      actor ! GetState

      expectMsg(State[Set[Int]](initialMarking, Set.empty))

      // fire the first transition (t1) manually
      actor ! FireTransition(t1, ())

      // expect the next marking: p2 -> 1
      expectMsgPF() { case TransitionFired(t1.id, _, _, result, _) if result == Marking(p2 -> 1) => }

      // since t2 fires automatically we also expect the next marking: p3 -> 1
      expectMsgPF() { case TransitionFired(t2.id, _, _, result, _) if result == Marking(p3 -> 1) => }

      // terminate the actor
      watch(actor)
      actor ! PoisonPill
      expectMsgClass(classOf[Terminated])

      // create a new actor with the same persistent identifier
      val newActor =
        system.actorOf(Props(new PetriNetProcess[Set[Int]](petriNet, initialMarking, Set.empty)), actorName)

      newActor ! GetState

      // assert that the marking is the same as before termination
      expectMsg(State[Set[Int]](Marking(p3 -> 1), Set(1, 2)))
    }

    "only fire one transition when two (or more) transitions compete for the same token" in {}

    "fire automatic transitions in paralallel when possible" in {

      def id[S, E]: S => E => S = s => e => s

      val p1 = Place[Unit](1, "p1")
      val p2 = Place[Unit](2, "p2")

      val t1 = nullTransition(1, "t1", isManaged = false)
      val t2 = stateTransition[Unit, Unit](id[Unit, Unit], unit => Thread.sleep(500), 2, "t2", isManaged = true)
      val t3 = stateTransition[Unit, Unit](id[Unit, Unit], unit => Thread.sleep(500), 3, "t3", isManaged = true)

      val petriNet = process[Unit](t1 ~> p1, t1 ~> p2, p1 ~> t2, p2 ~> t3)

      // creates a petri net actor with initial marking: p1 -> 1
      val initialMarking = Marking.empty

      val actor = system.actorOf(
        Props(new PetriNetProcess[Unit](petriNet, initialMarking, ())),
        java.util.UUID.randomUUID().toString
      )

      // fire the first transition manually
      actor ! FireTransition(1, ())

      expectMsgPF() { case TransitionFired(t1.id, _, _, result, _) => }

      import org.scalatest.concurrent.Timeouts._

      failAfter(Span(1000, Milliseconds)) {

        // expect that the two subsequent transitions are fired automatically and in parallel (in any order)
        expectMsgInAnyOrderPF(
          { case TransitionFired(`t2`, _, _, _, _) => },
          { case TransitionFired(`t3`, _, _, _, _) => }
        )
      }
    }
  }
}
