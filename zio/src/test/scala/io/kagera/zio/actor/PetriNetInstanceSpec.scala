package io.kagera.zio.actor

import java.io.File
import java.util.UUID
import io.kagera.api.colored._
import io.kagera.execution.Instance
import io.kagera.zio.actor.PetriNetInstanceProtocol._
import zio.{ Chunk, RIO }
import zio.actors.persistence.PersistenceId.PersistenceId
import zio.actors.{ ActorRef, ActorSystem, Supervisor }
import zio.clock.Clock
import zio.console.putStrLn
import zio.duration.durationInt
import zio.test.Assertion._
import zio.test.TestAspect.{ ignore, timeout }
import zio.test._
import zio.test.environment.TestEnvironment

object PetriNetInstanceSpec extends DefaultRunnableSpec {
  val configFile = Some(new File("./zio/src/test/resources/application.conf"))
  def createPetriNetActor[S](
    actorSystem: ActorSystem,
    petriNet: ExecutablePetriNet[S],
    persistenceId: String,
    processId: String = UUID.randomUUID().toString
  ): RIO[Clock, ActorRef[Message]] = for {
    actor <- actorSystem
      .make[Clock, Option[Instance[S]], Message](
        s"actor-$processId",
        Supervisor.none,
        None,
        PetriNetInstance.props[S](petriNet, PersistenceId.apply(persistenceId))
      )
  } yield actor
  def spec: ZSpec[TestEnvironment, Throwable] =
    suite("A persistent petri net actor")(
      testM("Respond with an Initialized response after processing an Initialized command") {
        val sequenceNet = new TestSequenceNet {
          override val sequence = Seq(transition()(_ => Added(1)), transition()(_ => Added(2)))
        }
        import sequenceNet._
        for {
          actorSystem <- ActorSystem(s"testsystem1", configFile)
          actor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, "1")
          res <- actor ? SetMarkingAndState(initialMarking, Set(1, 2, 3))
        } yield assert(res)(equalTo(Initialized(initialMarking, Set(1, 2, 3))))
      } @@ timeout(60.seconds),
      testM("Before being initialized respond with an empty InstanceState message on receiving a GetState command") {
        val sequenceNet = new TestSequenceNet {
          override val sequence = Seq(transition()(_ => Added(1)), transition()(_ => Added(2)))
        }
        import sequenceNet._
        for {
          actorSystem <- ActorSystem(s"testsystem2", configFile)
          actor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, "2")
          res <- actor ? GetState[Set[Int]]()
        } yield assert(res)(isSubtype[InstanceState[Set[Int]]](hasMarking(Marking.empty)))
      } @@ timeout(60.seconds),
      testM("After being initialized respond with an InstanceState message on receiving a GetState command") {
        val sequenceNet = new TestSequenceNet {
          override val sequence = Seq(transition()(_ => Added(1)), transition()(_ => Added(2)))
        }
        import sequenceNet._
        for {
          actorSystem <- ActorSystem(s"testsystem3", configFile)
          actor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, "3")
          res1 <- actor ? SetMarkingAndState(initialMarking, Set(1, 2, 3))
          res2 <- actor ? GetState[Set[Int]]()
        } yield assert(res1)(isSubtype[Initialized[_]](anything)) && assert(res2)(isSubtype[InstanceState[_]](anything))
      } @@ timeout(60.seconds),
      testM("Respond with a TransitionFailed message if a transition failed to fire") {
        val sequenceNet = new TestSequenceNet {
          override val sequence = Seq(
            transition()(_ => throw new RuntimeException("t1 failed!")),
            transition()(_ => throw new RuntimeException("t2 failed!"))
          )
        }
        import sequenceNet._
        for {
          actorSystem <- ActorSystem(s"testsystem4", configFile)
          actor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, "4")
          res1 <- actor ? SetMarkingAndState[Set[Int]](initialMarking, Set.empty)
          res2 <- actor ? FireTransition(1, ())
          res3 <- res2.runHead
        } yield assert(res1)(isSubtype[Initialized[_]](anything)) &&
          assert(res3)(isSome(isSubtype[TransitionFailed](anything)))
      } @@ timeout(60.seconds) @@ ignore,
      testM(
        "Respond with a TransitionNotEnabled message if a transition is not enabled because of a previous failure"
      ) {
        val sequenceNet = new TestSequenceNet {
          override val sequence =
            Seq(transition()(_ => throw new RuntimeException("t1 failed!")), transition()(_ => Added(2)))
        }
        import sequenceNet._
        for {
          actorSystem <- ActorSystem(s"testsystem5", configFile)
          actor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, "5")
          _ <- actor ! SetMarkingAndState(initialMarking, Set.empty)
          res1 <- actor ? FireTransition(1, ())
          res2 <- actor ? FireTransition(1, ())
          res3 <- res1.runHead
          res4 <- res2.runHead
        } yield assert(res3)(isSome(isSubtype[TransitionFailed](hasTransitionId(1L)))) &&
          assert(res4)(isSome(isSubtype[TransitionNotEnabled](hasTransitionId(1L))))
      } @@ timeout(60.seconds) @@ ignore,
      testM(
        "Respond with a TransitionNotEnabled message if a transition is not enabled because of not enough consumable tokens"
      ) {
        val transitionId = 2L
        val sequenceNet = new TestSequenceNet {
          override val sequence = Seq(transition()(_ => Added(1)), transition()(_ => Added(2)))
        }
        import sequenceNet._
        for {
          actorSystem <- ActorSystem(s"testsystem6", configFile)
          actor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, "6")
          _ <- actor ! SetMarkingAndState(initialMarking, Set.empty)
          // attempt to fire the second transition
          res <- actor ? FireTransition(transitionId, ())
          res1 <- res.runHead
          // expect a failure message
        } yield assert(res1)(isSome(isSubtype[TransitionNotEnabled](hasTransitionId(transitionId))))
      } @@ timeout(60.seconds),
      /*
        testM ("Retry to execute a transition with a delay when the exception strategy indicates so") {

          val retryHandler: TransitionExceptionHandler = {
            case (e, n) if n < 3 => RetryWithDelay((10 * Math.pow(2, n)).toLong)
            case _ => Fatal
          }

          val sequenceNet = new TestSequenceNet {
            override val sequence = Seq(
              transition(exceptionHandler = retryHandler) { _ => throw new RuntimeException("t1 failed") },
              transition() { _ => Added(2) }
            )
          }
          import sequenceNet._

          val id = UUID.randomUUID()

          for {
            actor <- createPetriNetActor[Set[Int]](petriNet)
            res <- actor ? foo
          } yield assert(res)(equalTo())

          actor ! Initialize(initialMarking, Set.empty)
          expectMsgClass(classOf[Initialized[_]])

          actor ! FireTransition(1, ())

          // expect 3 failure messages
          expectMsgPF() { case TransitionFailed(1, _, _, _, RetryWithDelay(20)) => }
          expectMsgPF() { case TransitionFailed(1, _, _, _, RetryWithDelay(40)) => }
          expectMsgPF() { case TransitionFailed(1, _, _, _, Fatal) => }

          // attempt to fire t1 explicitly
          actor ! FireTransition(1, ())

          // expect the transition to be not enabled
          val msg = expectMsgClass(classOf[TransitionNotEnabled])
        }

       */
      testM("Be able to restore it's state from persistent storage after termination") {

        val sequenceNet = new TestSequenceNet {
          override val sequence = Seq(transition()(_ => Added(1)), transition(automated = true)(_ => Added(2)))
        }
        import sequenceNet._

        val persistenceId = "8"

        for {
          actorSystem <- ActorSystem(s"testsystem$persistenceId", configFile)
          actor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, persistenceId, processId = persistenceId)
          _ <- actor.path.flatMap(p => putStrLn(p))
          init <- actor ? SetMarkingAndState(initialMarking, Set.empty)
          _ <- putStrLn(init.toString)
          res0 <- actor ? GetState[Set[Int]]()
          _ <- putStrLn(res0.toString)
          // fire the first transition (t1) manually
          resS <- actor ? FireTransition(1, ())
          res1 <- resS.runCollect
          _ <- putStrLn(res1.toString)
          // validate the final state
          res2 <- actor ? GetState[Set[Int]]()
          _ <- putStrLn(res2.toString)
          // terminate the actor
          remainingTasks <- actor.stop
          _ <- putStrLn(remainingTasks.toString)
          // create a new actor with the same persistent identifier
          newActor <- createPetriNetActor[Set[Int]](actorSystem, petriNet, persistenceId, processId = persistenceId)
          _ <- newActor.path.flatMap(p => putStrLn(p))
          res3 <- newActor ? GetState[Set[Int]]()
          _ <- putStrLn(res3.toString)
        } yield {
          // expect the next marking: p2 -> 1
          assert(res1)(
            isSubtype[Chunk[TransitionFired[_]]](
              hasFirst(hasTransitionId(1L) && hasResult(hasMarking(Marking(place(2) -> 1))))
            )
          ) &&
          // since t2 fires automatically we also expect the next marking: p3 -> 1
          assert(res2)(
            equalTo(
              InstanceState[Set[Int]](
                3,
                Marking(place(3) -> 1),
                Some(Set(1, 2)),
                Map.empty,
                Set(sequenceNet.transitions.findById(2L).get)
              )
            )
          ) &&
          // assert that the actor is the same as before termination
          assert(res3)(
            equalTo(
              InstanceState[Set[Int]](
                3,
                Marking(place(3) -> 1),
                Some(Set(1, 2)),
                Map.empty,
                Set(sequenceNet.transitions.findById(2L).get)
              )
            )
          )
        }

        //_ <- expectMsgPF(res1) { case TransitionFired(1, _, _, result) if result.marking == Marking(place(2) -> 1) => }
        // TODO: We're expecting another TransitionFired here! _ <- expectMsgPF(res1) { case TransitionFired(2, _, _, result) if result.marking == Marking(place(3) -> 1) => }
      } @@ timeout(10.seconds) @@ ignore
      /*
      testM("Not re-fire a failed/blocked transition after being restored from persistent storage") {

        val sequenceNet = new TestSequenceNet {
          override val sequence = Seq(
            transition(automated = true)(_ => Added(1)),
            transition(automated = true)(_ => throw new RuntimeException("t2 failed"))
          )
        }
        import sequenceNet._

        val actorName = java.util.UUID.randomUUID().toString

        val actor = createPetriNetActor[Set[Int]](petriNet, actorName)

        actor ! Initialize(initialMarking, Set.empty)
        expectMsgClass(classOf[Initialized[_]])

        // expect the next marking: p2 -> 1
        expectMsgPF() { case TransitionFired(1, _, _, _) => }
        expectMsgPF() { case TransitionFailed(2, _, _, _, BlockTransition) => }

        // terminate the actor
        watch(actor)
        actor ! PoisonPill
        expectMsgClass(classOf[Terminated])

        // create a new actor with the same persistent identifier
        val newActor = createPetriNetActor[Set[Int]](petriNet, actorName)

        // TODO assert t2 is not fired again using mocks

        newActor ! GetState

        // assert that the actor is the same as before termination
        expectMsgPF() { case InstanceState(2, marking, _, jobs) =>

        }
      },
      testM("When Idle terminate after some time if an idle TTL has been specified") {

        val ttl = 500 milliseconds

        val customSettings = Settings(evaluationStrategy = ExecutionContext.Implicits.global, idleTTL = Some(ttl))

        val sequenceNet = new TestSequenceNet {
          override val sequence =
            Seq(transition(automated = false)(_ => Added(1)), transition(automated = false)(_ => Added(2)))
        }
        import sequenceNet._

        val actor = system.actorOf(
          props = sequenceNetInstance.props(petriNet, customSettings),
          name = java.util.UUID.randomUUID().toString
        )

        actor ! Initialize(initialMarking, ())
        expectMsgClass(classOf[Initialized[_]])

        watch(actor)
        expectMsgClass(classOf[Terminated])
      },
      testM("fire automated transitions in parallel when possible") {

        val sequenceNet = new TestSequenceNet {
          override val eventSourcing: Unit => Unit => Unit = s => e => s
        }
        import sequenceNet._

        val p1 = Place[Unit](id = 1)
        val p2 = Place[Unit](id = 2)

        val t1 = nullTransition[Unit](id = 1, automated = false)
        val t2 = transition(id = 2, automated = true)(_ => Thread.sleep(500))
        val t3 = transition(id = 3, automated = true)(_ => Thread.sleep(500))

        val sequenceNet = createPetriNet(t1 ~> p1, t1 ~> p2, p1 ~> t2, p2 ~> t3)

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
       */
    )

  private def hasResult(assertion: Assertion[HasMarking]): Assertion[TransitionFired[_]] =
    hasField("result", _.result, assertion)
  private def hasMarking(marking: Marking): Assertion[HasMarking] = {
    hasField("marking", _.marking, equalTo(marking))
  }

  private def hasTransitionId(transitionId: Long): Assertion[TransitionResponse] = {
    hasField("transitionId", _.transitionId, equalTo(transitionId))
  }
}
