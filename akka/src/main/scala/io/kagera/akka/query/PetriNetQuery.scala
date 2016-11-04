package io.kagera.akka.query

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.scaladsl._
import akka.stream._
import akka.stream.scaladsl._
import io.kagera.akka.actor.{ AkkaObjectSerializer, PetriNetInstance }
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.execution.EventSourcing._
import io.kagera.execution._
import io.kagera.persistence.EventSerializer

import scala.concurrent.ExecutionContextExecutor

trait ConfiguredActorSystem {

  implicit def system: ActorSystem
  implicit def executor: ExecutionContextExecutor = system.dispatcher
  implicit def materializer = ActorMaterializer()
}

trait PetriNetQuery[S] extends ConfiguredActorSystem with EventSerializer[S] with AkkaObjectSerializer {

  implicit class SourceAdditions[+Out, +Mat](source: Source[Out, Mat]) {
    def foldMap[S, E](zero: S)(fn: (S, Out) => (S, E)): Source[E, Mat] =
      source
        .scan[(S, E)]((zero, null.asInstanceOf[E])) { case ((s, prev), e) =>
          fn(s, e)
        }
        .map(_._2)
  }

  def readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery

  def events(processId: String, topology: ExecutablePetriNet[S]): Source[Event, NotUsed] = {
    val src = readJournal.currentEventsByPersistenceId(
      PetriNetInstance.petriNetInstancePersistenceId(processId),
      0,
      Long.MaxValue
    )

    src.foldMap(Instance.uninitialized[S](topology)) { (instance, e) =>
      val deserializedEvent = deserializeEvent(instance)(e)
      val updatedInstance = applyEvent(deserializedEvent).runS(instance).value
      (updatedInstance, deserializedEvent)
    }
  }
}
