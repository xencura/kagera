package io.kagera.akka

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.{ CurrentEventsByPersistenceIdQuery, ReadJournal }
import akka.testkit.{ ImplicitSender, TestKit }
import io.kagera.akka.actor.PetriNetInstanceProtocol.{ Initialize, Initialized, TransitionFired }
import io.kagera.akka.query.PetriNetQuery
import io.kagera.api.colored.{ Marking, Place }
import org.scalatest.WordSpecLike
import io.kagera.api.colored.dsl._

import scala.concurrent.duration._

class QuerySpec
    extends TestKit(ActorSystem("QuerySpec", PetriNetInstanceSpec.config))
    with WordSpecLike
    with ImplicitSender {

  val timeOut: Duration = 2 seconds

  "The query package" should {

    "Return a source of events for a petri net instance" in new PetriNetQuery[Unit] {

      override implicit val system = QuerySpec.this.system

      override def readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery =
        PersistenceQuery(system)
          .readJournalFor("inmemory-read-journal")
          .asInstanceOf[ReadJournal with CurrentEventsByPersistenceIdQuery]

      val p1 = Place[Unit](id = 1)
      val p2 = Place[Unit](id = 2)
      val p3 = Place[Unit](id = 3)
      val t1 = nullTransition(id = 1, automated = true)
      val t2 = nullTransition(id = 2, automated = true)

      val petriNet = process(p1 ~> t1, t1 ~> p2, p2 ~> t2, t2 ~> p3)

      val processId = java.util.UUID.randomUUID().toString

      val instance = PetriNetInstanceSpec.createPetriNetActor(petriNet, processId)

      instance ! Initialize(Marking(p1 -> 1), ())
      expectMsg(Initialized(Marking(p1 -> 1), ()))
      expectMsgPF(timeOut) { case e: TransitionFired[_] if e.transitionId == t1.id => }
      expectMsgPF(timeOut) { case e: TransitionFired[_] if e.transitionId == t2.id => }
    }
  }
}
