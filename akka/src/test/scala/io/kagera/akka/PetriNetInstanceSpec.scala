package io.kagera.akka

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Terminated }
import akka.testkit.{ ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import io.kagera.akka.PetriNetInstanceSpec._
import io.kagera.akka.actor.PetriNetInstance
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api.colored.ExceptionStrategy.{ Fatal, RetryWithDelay }
import io.kagera.api.colored._
import io.kagera.api.colored.dsl.{ SequenceNet, _ }
import org.scalatest.time.{ Milliseconds, Span }
import org.scalatest.{ BeforeAndAfterAll, WordSpecLike }

object PetriNetInstanceSpec {

  val config = ConfigFactory.parseString("""
      |
      |akka {
      |  loggers = ["akka.testkit.TestEventListener"]
      |  persistence {
      |    journal.plugin = "inmemory-journal"
      |    snapshot-store.plugin = "inmemory-snapshot-store"
      |  }
      |}
      |
      |inmemory-read-journal {
      |  write-plugin = "inmemory-journal"
      |  offset-mode = "sequence"
      |  ask-timeout = "10s"
      |  refresh-interval = "50ms"
      |  max-buffer-size = "100"
      |}
      |
      |logging.root.level = WARN
    """.stripMargin)

  sealed trait Event
  case class Added(n: Int) extends Event
  case class Removed(n: Int) extends Event

  def createPetriNetActor[S](petriNet: ExecutablePetriNet[S], processId: String = UUID.randomUUID().toString)(implicit
    system: ActorSystem
  ) =
    system.actorOf(PetriNetInstance.props(petriNet), processId)
}

class PetriNetInstanceSpec
    extends TestKit(ActorSystem("PetriNetInstanceSpec", PetriNetInstanceSpec.config))
    with WordSpecLike
    with ImplicitSender
    with BeforeAndAfterAll {

  override def afterAll = system.shutdown()

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

  val integerSetEventSource: Set[Int] => Event => Set[Int] = set => {
    case Added(c) => set + c
    case Removed(c) => set - c
  }

  val p1 = Place[Unit](id = 1)
  val p2 = Place[Unit](id = 2)
  val p3 = Place[Unit](id = 3)

  "A persistent petri net actor" should {

    "Respond with an Initialized response after being initialized by an Initialized command" in new SequenceNet[Set[
      Int
    ], Event] {

      override val eventSourcing = integerSetEventSource

      override def sequence = {
        case 1 => transition()(_ => Added(1))
        case 2 => transition()(_ => Added(2))
      }

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set(1, 2, 3))

      expectMsg(Initialized(initialMarking, Set(1, 2, 3)))
    }

    "Afer being initialized respond with an InstanceState message on receiving a GetState command" in new SequenceNet[
      Set[Int],
      Event
    ] {

      override val eventSourcing = integerSetEventSource

      override def sequence = {
        case 1 => transition()(_ => Added(1))
        case 2 => transition()(_ => Added(2))
      }

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set(1, 2, 3))
      expectMsgClass(classOf[Initialized[_]])

      actor ! GetState
      expectMsgPF() { case InstanceState(_, _, _, _) => }

    }

    "Respond with a TransitionFailed message if a transition failed to fire" in new SequenceNet[Set[Int], Event] {

      override val eventSourcing = integerSetEventSource

      override def sequence = {
        case 1 => transition()(_ => throw new RuntimeException("t1 failed!"))
        case 2 => transition()(_ => throw new RuntimeException("t2 failed!"))
      }

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      actor ! FireTransition(1, ())

      expectMsgClass(classOf[TransitionFailed])
    }

    "Respond with a TransitionNotEnabled message if a transition is not enabled because of a previous failure" in new SequenceNet[
      Set[Int],
      Event
    ] {

      override val eventSourcing = integerSetEventSource

      override def sequence = {
        case 1 => transition()(_ => throw new RuntimeException("t1 failed!"))
        case 2 => transition()(_ => throw new RuntimeException("t2 failed!"))
      }

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      actor ! FireTransition(1, ())

      expectMsgPF() { case TransitionFailed(1, _, _, _, _) => }

      actor ! FireTransition(1, ())

      // expect a failure message
      expectMsgPF() { case TransitionNotEnabled(1, msg) => }
    }

    "Respond with a TransitionNotEnabled message if a transition is not enabled because of not enough consumable tokens" in new SequenceNet[
      Set[Int],
      Event
    ] {

      override val eventSourcing = integerSetEventSource

      override def sequence = {
        case 1 => transition()(_ => Added(1))
        case 2 => transition()(_ => Added(2))
      }

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      // attempt to fire the second transition
      actor ! FireTransition(2, ())

      // expect a failure message
      expectMsgPF() { case TransitionNotEnabled(2, _) => }
    }

    "Retry to execute a transition with a delay when the exception strategy indicates so" in new SequenceNet[Set[
      Int
    ], Event] {

      override val eventSourcing = integerSetEventSource

      val retryHandler: TransitionExceptionHandler = {
        case (e, n) if n < 3 => RetryWithDelay((10 * Math.pow(2, n)).toLong)
        case _ => Fatal
      }

      override def sequence = {
        case 1 => transition(exceptionHandler = retryHandler) { _ => { throw new RuntimeException("t1 failed") } }
        case 2 => transition() { _ => Added(2) }
      }

      val id = UUID.randomUUID()

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      actor ! FireTransition(1, ())

      // expect 3 failure messages
      expectMsgPF() { case TransitionFailed(1, _, _, _, RetryWithDelay(20)) => }
      expectMsgPF() { case TransitionFailed(1, _, _, _, RetryWithDelay(40)) => }
      expectMsgPF() { case TransitionFailed(1, _, _, _, Fatal) => }

      // attempt to fire t1 explicitely
      actor ! FireTransition(1, ())

      // expect the transition to be not enabled
      val msg = expectMsgClass(classOf[TransitionNotEnabled])
    }

    "Be able to restore it's state after termination" in new SequenceNet[Set[Int], Event] {

      override val eventSourcing = integerSetEventSource

      override def sequence = {
        case 1 => transition()(_ => Added(1))
        case 2 => transition(automated = true)(_ => Added(2))
      }

      val actorName = java.util.UUID.randomUUID().toString

      val actor = createPetriNetActor[Set[Int]](petriNet, actorName)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      // fire the first transition (t1) manually
      actor ! FireTransition(1, ())

      // expect the next marking: p2 -> 1
      expectMsgPF() { case TransitionFired(1, _, _, result) if result.marking == Marking(p2 -> 1) => }

      // since t2 fires automatically we also expect the next marking: p3 -> 1
      expectMsgPF() { case TransitionFired(2, _, _, result) if result.marking == Marking(p3 -> 1) => }

      // validate the final state
      actor ! GetState
      expectMsg(InstanceState[Set[Int]](3, Marking(p3 -> 1), Set(1, 2), Map.empty))

      // terminate the actor
      watch(actor)
      actor ! PoisonPill
      expectMsgClass(classOf[Terminated])

      // create a new actor with the same persistent identifier
      val newActor = createPetriNetActor[Set[Int]](petriNet, actorName)

      newActor ! GetState

      // assert that the marking is the same as before termination
      expectMsg(InstanceState[Set[Int]](3, Marking(p3 -> 1), Set(1, 2), Map.empty))
    }

    "fire automated transitions in parallel when possible" in new StateTransitionNet[Unit, Unit] {

      override val eventSourcing: Unit => Unit => Unit = s => e => s

      val p1 = Place[Unit](id = 1)
      val p2 = Place[Unit](id = 2)

      val t1 = nullTransition[Unit](id = 1, automated = false)
      val t2 = transition(id = 2, automated = true)(unit => Thread.sleep(500))
      val t3 = transition(id = 3, automated = true)(unit => Thread.sleep(500))

      val petriNet = createPetriNet(t1 ~> p1, t1 ~> p2, p1 ~> t2, p2 ~> t3)

      // creates a petri net actor with initial marking: p1 -> 1
      val initialMarking = Marking.empty

      val actor = createPetriNetActor(petriNet)

      actor ! Initialize(initialMarking, ())
      expectMsgClass(classOf[Initialized[_]])

      // fire the first transition manually
      actor ! FireTransition(1, ())

      expectMsgPF() { case TransitionFired(1, _, _, _) => }

      import org.scalatest.concurrent.Timeouts._

      failAfter(Span(1000, Milliseconds)) {

        // expect that the two subsequent transitions are fired automatically and in parallel (in any order)
        expectMsgInAnyOrderPF({ case TransitionFired(2, _, _, _) => }, { case TransitionFired(3, _, _, _) => })
      }
    }
  }
}
