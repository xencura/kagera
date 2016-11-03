package io.kagera.akka.query

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query._
import akka.persistence.query.scaladsl._
import akka.stream._
import akka.stream.scaladsl._
import io.kagera.akka.actor.PetriNetInstance
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.execution.EventSourcing.Event
import io.kagera.persistence.EventSerializer

import scala.concurrent.ExecutionContextExecutor

import io.kagera.execution._

object PetriNetQuery {}

trait ConfiguredActorSystem {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer = ActorMaterializer()
}

trait PetriNetQuery[S] {
  this: ConfiguredActorSystem with EventSerializer[S] =>

  def readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery

  def events(processId: String, topology: ExecutablePetriNet[S]): Source[Event, NotUsed] = {
    val src = readJournal.currentEventsByPersistenceId(
      PetriNetInstance.petriNetInstancePersistenceId(processId),
      0,
      Long.MaxValue
    )

    val unitializedInstance = Instance.uninitialized[S](topology)
    src.map[Event] { e: EventEnvelope =>
      ???
    }
  }
}
