package io.kagera.akka

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Terminated }
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import io.kagera.akka.PersistentPetriNetActorSpec._
import io.kagera.akka.actor.PetriNetProcess
import io.kagera.akka.actor.PetriNetProcessProtocol._
import io.kagera.api.colored.ExceptionStrategy.{ Fatal, RetryWithDelay }
import io.kagera.api.colored._
import io.kagera.api.colored.dsl._
import org.scalatest.WordSpecLike
import org.scalatest.time.{ Milliseconds, Span }

object PersistentPetriNetActorSpec {

  val config = ConfigFactory.parseString("""
      |akka {
      |  loggers = ["akka.testkit.TestEventListener"]
      |
      |  persistence.journal.plugin = "akka.persistence.journal.inmem"
      |  persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |}
      |
      |logging.root.level = WARN
    """.stripMargin)

  sealed trait Event
  case class Added(n: Int) extends Event
  case class Removed(n: Int) extends Event
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

  def createPetriNetActor[S](
    petriNet: ExecutablePetriNet[S],
    initialMarking: Marking,
    state: S,
    actorName: String = UUID.randomUUID().toString
  ) =
    system.actorOf(PetriNetProcess.props(petriNet, initialMarking, state), actorName)

  val integerSetEventSource: Set[Int] => Event => Set[Int] = set => {
    case Added(c) => set + c
    case Removed(c) => set - c
  }

  val p1 = Place[Unit](id = 1, label = "p1")
  val p2 = Place[Unit](id = 2, label = "p2")
  val p3 = Place[Unit](id = 3, label = "p3")

  import system.dispatcher

  "A persistent petri net actor" should {

    "Respond with a TransitionFailed message if a transition failed to fire" in new StateTransitionNet[
      Set[Int],
      Event
    ] {

      override val eventSourcing = integerSetEventSource

      val t1 = transition(id = 1)(set => throw new RuntimeException("something went wrong"))

      val petriNet = createPetriNet(p1 ~> t1, t1 ~> p2)

      val initialMarking = Marking(p1 -> 1)
      val actor = createPetriNetActor[Set[Int]](petriNet, initialMarking, Set.empty)

      actor ! FireTransition(t1, ())

      expectMsgClass(classOf[TransitionFailed])
    }

    "Respond with a TransitionNotEnabled message if a transition is not enabled because of a previous failure" in new StateTransitionNet[
      Set[Int],
      Event
    ] {
      override val eventSourcing = integerSetEventSource

      val t1 = transition(id = 1)(set => throw new RuntimeException("something went wrong"))

      val petriNet = createPetriNet(p1 ~> t1, t1 ~> p2)

      val initialMarking = Marking(p1 -> 1)
      val actor = createPetriNetActor[Set[Int]](petriNet, initialMarking, Set.empty)

      actor ! FireTransition(t1, ())

      expectMsgClass(classOf[TransitionFailed])

      actor ! FireTransition(t1, ())

      // expect a failure message
      expectMsgPF() { case TransitionNotEnabled(t1.id, msg) => println(msg) }
    }

    "Respond with a TransitionNotEnabled message if a transition is not enabled because of not enough consumable tokens" in new StateTransitionNet[
      Set[Int],
      Event
    ] {

      override val eventSourcing = integerSetEventSource

      val t1 = transition(id = 1)(set => Added(1))
      val t2 = transition(id = 2)(set => Added(2))

      val petriNet = createPetriNet(p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)

      // creates a petri net actor with initial marking: p1 -> 1
      val initialMarking = Marking(p1 -> 1)

      val actor = createPetriNetActor[Set[Int]](petriNet, initialMarking, Set.empty)

      // attempt to fire the second transition
      actor ! FireTransition(t2)

      // expect a failure message
      expectMsgPF() { case TransitionNotEnabled(t2.id, _) => }
    }

    "Retry to execute a transition with a delay when the exception strategy indicates so" in new StateTransitionNet[Set[
      Int
    ], Event] {

      override val eventSourcing = integerSetEventSource

      val retryHandler: TransitionExceptionHandler = {
        case (e, n) if n < 3 => RetryWithDelay((10 * Math.pow(2, n)).toLong)
        case _ => Fatal
      }

      val t1 = transition(id = 1, exceptionStrategy = retryHandler) { set =>
        { throw new RuntimeException("something went wrong") }
      }

      val petriNet = createPetriNet(p1 ~> t1, t1 ~> p2)

      val id = UUID.randomUUID()
      val initialMarking = Marking(p1 -> 1)

      val actor = createPetriNetActor[Set[Int]](petriNet, initialMarking, Set.empty)

      actor ! FireTransition(t1)

      // expect 3 failure messages
      expectMsgPF() { case TransitionFailed(t1.id, _, _, _, RetryWithDelay(20)) => }
      expectMsgPF() { case TransitionFailed(t1.id, _, _, _, RetryWithDelay(40)) => }
      expectMsgPF() { case TransitionFailed(t1.id, _, _, _, Fatal) => }

      // attempt to fire t1 explicitely
      actor ! FireTransition(t1)

      // expect the transition to be not enabled
      val msg = expectMsgClass(classOf[TransitionNotEnabled])
      println(s"msg: $msg")
    }

    "Be able to restore it's state after termination" in new StateTransitionNet[Set[Int], Event] {

      override val eventSourcing = integerSetEventSource

      val actorName = java.util.UUID.randomUUID().toString

      val t1 = transition(id = 1) { set =>
        Added(1)
      }
      val t2 = transition(id = 2, isManaged = true) { set =>
        Added(2)
      }

      val petriNet = process[Set[Int]](p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)

      // creates a petri net actor with initial marking: p1 -> 1
      val initialMarking = Marking(p1 -> 1)

      val actor = createPetriNetActor[Set[Int]](petriNet, initialMarking, Set.empty, actorName)

      // assert that the actor is in the initial state
      actor ! GetState

      expectMsg(ProcessState[Set[Int]](1, initialMarking, Set.empty))

      // fire the first transition (t1) manually
      actor ! FireTransition(t1)

      // expect the next marking: p2 -> 1
      expectMsgPF() { case TransitionFired(t1.id, _, _, result, _) if result == Marking(p2 -> 1) => }

      // since t2 fires automatically we also expect the next marking: p3 -> 1
      expectMsgPF() { case TransitionFired(t2.id, _, _, result, _) if result == Marking(p3 -> 1) => }

      // terminate the actor
      watch(actor)
      actor ! PoisonPill
      expectMsgClass(classOf[Terminated])

      // create a new actor with the same persistent identifier
      val newActor = createPetriNetActor[Set[Int]](petriNet, initialMarking, Set.empty, actorName)

      newActor ! GetState

      // assert that the marking is the same as before termination
      expectMsg(ProcessState[Set[Int]](3, Marking(p3 -> 1), Set(1, 2)))
    }

    "fire automatic transitions in parallel when possible" in new StateTransitionNet[Unit, Unit] {

      override val eventSourcing: Unit => Unit => Unit = s => e => s

      val p1 = Place[Unit](1, "p1")
      val p2 = Place[Unit](2, "p2")

      val t1 = nullTransition(1, "t1", automated = false)
      val t2 = transition(id = 2, automated = true)(unit => Thread.sleep(500))
      val t3 = transition(id = 3, automated = true)(unit => Thread.sleep(500))

      val petriNet = createPetriNet(t1 ~> p1, t1 ~> p2, p1 ~> t2, p2 ~> t3)

      // creates a petri net actor with initial marking: p1 -> 1
      val initialMarking = Marking.empty

      val actor = createPetriNetActor(petriNet, initialMarking, ())

      // fire the first transition manually
      actor ! FireTransition(t1)

      expectMsgPF() { case TransitionFired(t1.id, _, _, result, _) => }

      import org.scalatest.concurrent.Timeouts._

      failAfter(Span(1000, Milliseconds)) {

        // expect that the two subsequent transitions are fired automatically and in parallel (in any order)
        expectMsgInAnyOrderPF(
          { case TransitionFired(t2.id, _, _, _, _) => },
          { case TransitionFired(t3.id, _, _, _, _) => }
        )
      }
    }
  }
}
