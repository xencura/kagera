package io.kagera.akka.actor

import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.api.colored.{ ExecutablePetriNet, Transition }
import io.kagera.execution.EventSourcing._
import io.kagera.execution.{ EventSourcing, Instance }
import io.kagera.persistence.{ messages, Serialization }

trait PetriNetInstanceRecovery[S, T <: Transition[_, _, S]] {

  this: PersistentActor =>

  def topology: ExecutablePetriNet[S, T]

  implicit val system = context.system
  val serializer = new Serialization(new AkkaObjectSerializer(context.system))

  def onRecoveryCompleted(state: Instance[S, T]): Unit

  def applyEvent(i: Instance[S, T])(e: Event): Instance[S, T] = EventSourcing.applyEvent[S, T](e).runS(i).value

  def persistEvent[R, E <: Event](instance: Instance[S, T], e: E)(fn: E => R): Unit = {
    val serializedEvent = serializer.serializeEvent(e)(instance)
    persist(serializedEvent) { persisted => fn.apply(e) }
  }

  private var recoveringState: Instance[S, T] = Instance.uninitialized[S, T](topology)

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
