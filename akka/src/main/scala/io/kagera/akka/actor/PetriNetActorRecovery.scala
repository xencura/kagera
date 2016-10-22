package io.kagera.akka.actor

import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.akka.actor.PetriNetEventSourcing._
import io.kagera.akka.actor.PetriNetExecution.Instance
import io.kagera.api.colored.ExecutablePetriNet

trait PetriNetActorRecovery[S] extends PetriNetEventAdapter[S] {

  this: PersistentActor =>

  def process: ExecutablePetriNet[S]

  override implicit def system = context.system

  def onRecoveryCompleted(state: Instance[S])

  def persistEvent[T, E <: Event](state: Instance[S], e: E)(fn: (Instance[S], E) => T): Unit = {
    val serializedEvent = serializeEvent(state)(e)
    val updatedState = applyEvent(e)(state)._1
    persist(serializedEvent) { persisted =>
      fn.apply(updatedState, e)
    }
  }

  private var recoveringState: Instance[S] = Instance.uninitialized[S](process)

  override def receiveRecover: Receive = {
    case e: io.kagera.akka.persistence.Initialized =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = applyEvent(deserializedEvent)(recoveringState)._1
    case e: io.kagera.akka.persistence.TransitionFired =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = applyEvent(deserializedEvent)(recoveringState)._1
    case RecoveryCompleted =>
      if (recoveringState.sequenceNr > 0)
        onRecoveryCompleted(recoveringState)
  }
}
