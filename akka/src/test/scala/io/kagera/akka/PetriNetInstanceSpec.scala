package io.kagera.akka

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Terminated }
import io.kagera.akka.PetriNetInstanceSpec._
import io.kagera.akka.actor.PetriNetInstance
import io.kagera.akka.actor.PetriNetInstance.Settings
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api.colored.ExceptionStrategy.{ BlockTransition, Fatal, RetryWithDelay }
import io.kagera.api.colored._
import io.kagera.api.colored.dsl._
import org.scalatest.time.{ Milliseconds, Span }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PetriNetInstanceSpec {

  def createPetriNetActor[S](petriNet: ExecutablePetriNet[S], processId: String = UUID.randomUUID().toString)(implicit system: ActorSystem) =
    system.actorOf(PetriNetInstance.props(petriNet), processId)
}

class PetriNetInstanceSpec extends AkkaTestBase {

  "A persistent petri net actor" should {

    "Respond with an Initialized response after processing an Initialized command" in new TestSequenceNet {

      override val sequence = Seq(
        transition()(_ ⇒ Added(1)),
        transition()(_ ⇒ Added(2))
      )

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set(1, 2, 3))
      expectMsg(Initialized(initialMarking, Set(1, 2, 3)))
    }

    "Before being intialized respond with an IllegalCommand message on receiving a GetState command" in new TestSequenceNet {

      override val sequence = Seq(
        transition()(_ ⇒ Added(1)),
        transition()(_ ⇒ Added(2))
      )

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! GetState
      expectMsgClass(classOf[IllegalCommand])
    }

    "Afer being initialized respond with an InstanceState message on receiving a GetState command" in new TestSequenceNet {

      override val sequence = Seq(
        transition()(_ ⇒ Added(1)),
        transition()(_ ⇒ Added(2))
      )

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set(1, 2, 3))
      expectMsgClass(classOf[Initialized[_]])

      actor ! GetState
      expectMsgPF() { case InstanceState(_, _, _, _) ⇒ }

    }

    "Respond with a TransitionFailed message if a transition failed to fire" in new TestSequenceNet {

      override val sequence = Seq(
        transition()(_ ⇒ throw new RuntimeException("t1 failed!")),
        transition()(_ ⇒ throw new RuntimeException("t2 failed!"))
      )

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      actor ! FireTransition(1, ())

      expectMsgClass(classOf[TransitionFailed])
    }

    "Respond with a TransitionNotEnabled message if a transition is not enabled because of a previous failure" in new TestSequenceNet {

      override val sequence = Seq(
        transition()(_ ⇒ throw new RuntimeException("t1 failed!")),
        transition()(_ ⇒ Added(2))
      )

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      actor ! FireTransition(1, ())

      expectMsgPF() { case TransitionFailed(1, _, _, _, _) ⇒ }

      actor ! FireTransition(1, ())

      // expect a failure message
      expectMsgPF() { case TransitionNotEnabled(1, msg) ⇒ }
    }

    "Respond with a TransitionNotEnabled message if a transition is not enabled because of not enough consumable tokens" in new TestSequenceNet {

      override val sequence = Seq(
        transition()(_ ⇒ Added(1)),
        transition()(_ ⇒ Added(2))
      )

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      // attempt to fire the second transition
      actor ! FireTransition(2, ())

      // expect a failure message
      expectMsgPF() { case TransitionNotEnabled(2, _) ⇒ }
    }

    "Retry to execute a transition with a delay when the exception strategy indicates so" in new TestSequenceNet {

      val retryHandler: TransitionExceptionHandler = {
        case (e, n) if n < 3 ⇒ RetryWithDelay((10 * Math.pow(2, n)).toLong)
        case _               ⇒ Fatal
      }

      override val sequence = Seq(
        transition(exceptionHandler = retryHandler) { _ ⇒ throw new RuntimeException("t1 failed") },
        transition() { _ ⇒ Added(2) }
      )

      val id = UUID.randomUUID()

      val actor = createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      actor ! FireTransition(1, ())

      // expect 3 failure messages
      expectMsgPF() { case TransitionFailed(1, _, _, _, RetryWithDelay(20)) ⇒ }
      expectMsgPF() { case TransitionFailed(1, _, _, _, RetryWithDelay(40)) ⇒ }
      expectMsgPF() { case TransitionFailed(1, _, _, _, Fatal) ⇒ }

      // attempt to fire t1 explicitly
      actor ! FireTransition(1, ())

      // expect the transition to be not enabled
      val msg = expectMsgClass(classOf[TransitionNotEnabled])
    }

    "Be able to restore it's state from persistent storage after termination" in new TestSequenceNet {

      override val sequence = Seq(
        transition()(_ ⇒ Added(1)),
        transition(automated = true)(_ ⇒ Added(2))
      )

      val actorName = java.util.UUID.randomUUID().toString

      val actor = createPetriNetActor[Set[Int]](petriNet, actorName)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      // fire the first transition (t1) manually
      actor ! FireTransition(1, ())

      // expect the next marking: p2 -> 1
      expectMsgPF() { case TransitionFired(1, _, _, result) if result.marking == Marking(place(2) -> 1) ⇒ }

      // since t2 fires automatically we also expect the next marking: p3 -> 1
      expectMsgPF() { case TransitionFired(2, _, _, result) if result.marking == Marking(place(3) -> 1) ⇒ }

      // validate the final state
      actor ! GetState
      expectMsg(InstanceState[Set[Int]](3, Marking(place(3) -> 1), Set(1, 2), Map.empty))

      // terminate the actor
      watch(actor)
      actor ! PoisonPill
      expectMsgClass(classOf[Terminated])
      Thread.sleep(100)

      // create a new actor with the same persistent identifier
      val newActor = createPetriNetActor[Set[Int]](petriNet, actorName)

      newActor ! GetState

      // assert that the actor is the same as before termination
      expectMsg(InstanceState[Set[Int]](3, Marking(place(3) -> 1), Set(1, 2), Map.empty))
    }

    "Not re-fire a failed/blocked transition after being restored from persistent storage" in new TestSequenceNet {

      override val sequence = Seq(
        transition(automated = true)(_ ⇒ Added(1)),
        transition(automated = true)(_ ⇒ throw new RuntimeException("t2 failed"))
      )

      val actorName = java.util.UUID.randomUUID().toString

      val actor = createPetriNetActor[Set[Int]](petriNet, actorName)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      // expect the next marking: p2 -> 1
      expectMsgPF() { case TransitionFired(1, _, _, _) ⇒ }
      expectMsgPF() { case TransitionFailed(2, _, _, _, BlockTransition) ⇒ }

      // terminate the actor
      watch(actor)
      actor ! PoisonPill
      expectMsgClass(classOf[Terminated])

      // create a new actor with the same persistent identifier
      val newActor = createPetriNetActor[Set[Int]](petriNet, actorName)

      // TODO assert t2 is not fired again using mocks

      newActor ! GetState

      // assert that the actor is the same as before termination
      expectMsgPF() {
        case InstanceState(2, marking, _, jobs) ⇒

      }
    }

    "When Idle terminate after some time if an idle TTL has been specified" in new TestSequenceNet {

      val ttl = 500 milliseconds

      val customSettings = Settings(
        evaluationStrategy = ExecutionContext.Implicits.global,
        idleTTL = Some(ttl)
      )

      override val sequence = Seq(
        transition(automated = false)(_ ⇒ Added(1)),
        transition(automated = false)(_ ⇒ Added(2))
      )

      val actor = system.actorOf(
        props = PetriNetInstance.props(petriNet, customSettings),
        name = java.util.UUID.randomUUID().toString)

      actor ! Initialize(initialMarking, ())
      expectMsgClass(classOf[Initialized[_]])

      watch(actor)
      expectMsgClass(classOf[Terminated])
    }

    "fire automated transitions in parallel when possible" in new StateTransitionNet[Unit, Unit] {

      override val eventSourcing: Unit ⇒ Unit ⇒ Unit = s ⇒ e ⇒ s

      val p1 = Place[Unit](id = 1)
      val p2 = Place[Unit](id = 2)

      val t1 = nullTransition[Unit](id = 1, automated = false)
      val t2 = transition(id = 2, automated = true)(_ ⇒ Thread.sleep(500))
      val t3 = transition(id = 3, automated = true)(_ ⇒ Thread.sleep(500))

      val petriNet = createPetriNet(
        t1 ~> p1,
        t1 ~> p2,
        p1 ~> t2,
        p2 ~> t3
      )

      // creates a petri net actor with initial marking: p1 -> 1
      val initialMarking = Marking.empty

      val actor = createPetriNetActor(petriNet)

      actor ! Initialize(initialMarking, ())
      expectMsgClass(classOf[Initialized[_]])

      // fire the first transition manually
      actor ! FireTransition(1, ())

      expectMsgPF() { case TransitionFired(1, _, _, _) ⇒ }

      import org.scalatest.concurrent.Timeouts._

      failAfter(Span(1000, Milliseconds)) {

        // expect that the two subsequent transitions are fired automatically and in parallel (in any order)
        expectMsgInAnyOrderPF(
          { case TransitionFired(2, _, _, _) ⇒ },
          { case TransitionFired(3, _, _, _) ⇒ }
        )
      }
    }
  }
}
