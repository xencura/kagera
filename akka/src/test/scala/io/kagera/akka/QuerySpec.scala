package io.kagera.akka

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, ReadJournal}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.{ ImplicitSender, TestKit }
import io.kagera.akka.actor.PetriNetInstanceProtocol.{ Initialize, Initialized, TransitionFired }
import io.kagera.akka.query.PetriNetQuery
import io.kagera.api.colored.dsl._
import io.kagera.api.colored.{ Marking, Place }
import io.kagera.execution.EventSourcing.{ InitializedEvent, TransitionFiredEvent }
import org.scalatest.Inside._
import org.scalatest.Matchers._
import org.scalatest.WordSpecLike

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

class QuerySpec extends TestKit(ActorSystem("QuerySpec", AkkaTestBase.defaultTestConfig))
    with WordSpecLike with ImplicitSender {

  val timeOut: Duration = 2 seconds

  implicit def materializer = ActorMaterializer()
  implicit def ec: ExecutionContext = system.dispatcher

  "The query package" should {

    "Return a source of events for a petri net instance" in new PetriNetQuery[Unit] {

      override def readJournal =
        PersistenceQuery(system).readJournalFor("inmemory-read-journal")
          .asInstanceOf[ReadJournal with CurrentEventsByPersistenceIdQuery]

      val p1 = Place[Unit](id = 1)
      val p2 = Place[Unit](id = 2)
      val p3 = Place[Unit](id = 3)
      val t1 = nullTransition[Unit](id = 1, automated = true)
      val t2 = nullTransition[Unit](id = 2, automated = true)

      val petriNet = process(p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)
      val processId = java.util.UUID.randomUUID().toString
      val instance = PetriNetInstanceSpec.createPetriNetActor(petriNet, processId)

      instance ! Initialize(Marking(p1 -> 1))
      expectMsg(Initialized(Marking(p1 -> 1), ()))
      expectMsgPF(timeOut) { case e: TransitionFired[_] if e.transitionId == t1.id => }
      expectMsgPF(timeOut) { case e: TransitionFired[_] if e.transitionId == t2.id => }

      // wait for all events to be available in the read journal
      Thread.sleep(100)

      val futureEventList = eventsForInstance(processId, petriNet).map(_._2).runWith(Sink.seq)
      val eventList = Await.result(futureEventList, 2 seconds).toList

      eventList.size shouldBe 3

      eventList(0) shouldBe InitializedEvent(
        marking = Marking(p1 -> 1),
        state = ())

      inside(eventList(1)) {
        case TransitionFiredEvent(_, transitionId, _, _, consumed, produced, _) =>
          transitionId shouldBe t1.id
          consumed shouldBe Marking(p1 -> 1)
          produced shouldBe Marking(p2 -> 1)
      }

      inside(eventList(2)) {
        case TransitionFiredEvent(_, transitionId, _, _, consumed, produced, _) =>
          transitionId shouldBe t2.id
          consumed shouldBe Marking(p2 -> 1)
          produced shouldBe Marking(p3 -> 1)
      }
    }
  }
}
