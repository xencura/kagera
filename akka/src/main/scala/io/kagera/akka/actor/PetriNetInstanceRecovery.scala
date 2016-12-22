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

  def persistEvent[T, E <: Event](instance: Instance[S], e: E)(fn: E => T): Unit = {
    val serializedEvent = serializer.serializeEvent(e)(instance)
    persist(serializedEvent) { persisted => fn.apply(e) }
  }

  private var recoveringState: Instance[S] = Instance.uninitialized[S](topology)

  private def applyToRecoveringState(e: AnyRef) = {
    val deserializedEvent = serializer.deserializeEvent(e)(recoveringState)
    recoveringState = applyEvent(recoveringState)(deserializedEvent)
  }

  override def receiveRecover: Receive = {
    case e: messages.Initialized => applyToRecoveringState(e)
    case e: messages.TransitionFired => applyToRecoveringState(e)
    case e: messages.TransitionFailed => applyToRecoveringState(e)
    case RecoveryCompleted =>
      if (recoveringState.sequenceNr > 0)
        onRecoveryCompleted(recoveringState)
  }
}
