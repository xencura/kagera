package io.kagera.akka.actor

import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.execution.EventSourcing._
import io.kagera.execution.{ EventSourcing, Instance }
import io.kagera.persistence.{ messages, Serialization }

trait PetriNetInstanceRecovery[S] {

  this: PersistentActor =>

  def topology: ExecutablePetriNet[S]

  implicit val system = context.system
  val serializer = new Serialization(new AkkaObjectSerializer(context.system))

  def onRecoveryCompleted(state: Instance[S])

  def applyEvent(i: Instance[S])(e: Event): Instance[S] = EventSourcing.applyEvent(e).runS(i).value

  def persistEvent[T, E <: Event](instance: Instance[S], e: E)(fn: (Instance[S], E) => T): Unit = {

    val serializedEvent = serializer.serializeEvent(e)(instance)
    val updatedState = applyEvent(instance)(e)
    persist(serializedEvent) { persisted =>
      fn.apply(updatedState, e)
    }
  }

  private var recoveringState: Instance[S] = Instance.uninitialized[S](topology)

  override def receiveRecover: Receive = {
    case e: messages.Initialized =>
      val deserializedEvent = serializer.deserializeEvent(e)(recoveringState)
      recoveringState = applyEvent(recoveringState)(deserializedEvent)
    case e: messages.TransitionFired =>
      val deserializedEvent = serializer.deserializeEvent(e)(recoveringState)
      recoveringState = applyEvent(recoveringState)(deserializedEvent)
    case RecoveryCompleted =>
      if (recoveringState.sequenceNr > 0)
        onRecoveryCompleted(recoveringState)
  }
}
