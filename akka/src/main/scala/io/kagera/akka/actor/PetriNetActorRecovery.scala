package io.kagera.akka.actor

import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.akka.actor.PetriNetEventSourcing._
import io.kagera.akka.actor.PetriNetExecution.ExecutionState
import io.kagera.api.colored.ExecutablePetriNet

trait PetriNetActorRecovery[S] extends PetriNetEventAdapter[S] {

  this: PersistentActor =>

  def process: ExecutablePetriNet[S]

  override implicit def system = context.system

  def onRecoveryCompleted(state: ExecutionState[S])

  def persistEvent[T, E <: Event](state: ExecutionState[S], e: E)(fn: (ExecutionState[S], E) => T): Unit = {
    val serializedEvent = serializeEvent(state)(e)
    val updatedState = PetriNetEventSourcing.apply(e)(state)
    persist(serializedEvent) { persisted =>
      fn.apply(updatedState, e)
    }
  }

  private var recoveringState: ExecutionState[S] = ExecutionState.uninitialized[S](process)

  override def receiveRecover: Receive = {
    case e: io.kagera.akka.persistence.Initialized =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = PetriNetEventSourcing.apply(deserializedEvent)(recoveringState)
    case e: io.kagera.akka.persistence.TransitionFired =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = PetriNetEventSourcing.apply(deserializedEvent)(recoveringState)
    case RecoveryCompleted =>
      if (recoveringState.sequenceNr > 0)
        onRecoveryCompleted(recoveringState)
  }
}
