package io.kagera.akka.query

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl._
import io.kagera.akka.actor.{ AkkaObjectSerializer, PetriNetInstance }
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.execution.EventSourcing._
import io.kagera.execution._
import io.kagera.persistence.EventSerializer

trait PetriNetQuery[S] {

  def readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery

  def eventsForInstance(instanceId: String, topology: ExecutablePetriNet[S])(implicit
    actorSystem: ActorSystem
  ): (Source[(Instance[S], Event), NotUsed]) = {

    val eventSerializer = new EventSerializer[S] with AkkaObjectSerializer {
      override def system: ActorSystem = actorSystem
    }

    val persistentId = PetriNetInstance.petriNetInstancePersistenceId(instanceId)
    val src = readJournal.currentEventsByPersistenceId(persistentId, 0, Long.MaxValue)

    src
      .scan[(Instance[S], Event)]((Instance.uninitialized(topology), null.asInstanceOf[Event])) {
        case ((instance, prev), e) =>
          val deserializedEvent = eventSerializer.deserializeEvent(instance)(e.event.asInstanceOf[AnyRef])
          val updatedInstance = applyEvent(deserializedEvent).runS(instance).value
          (updatedInstance, deserializedEvent)
      }
      .drop(1)
  }
}
