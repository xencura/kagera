package io.kagera.akka.actor

import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.akka.actor.PetriNetExecution.ExecutionState
import io.kagera.akka.actor.PetriNetProcess.{ Event, InitializedEvent, TransitionFailedEvent, TransitionFiredEvent }
import io.kagera.api.colored.ExecutablePetriNet

trait PetriNetActorRecovery[S] extends PetriNetEventAdapter[S] {

  this: PersistentActor =>

  def process: ExecutablePetriNet[S]

  override implicit def system = context.system

  def onRecoveryCompleted(state: ExecutionState[S])

  def deserializeEvent(state: ExecutionState[S]): AnyRef => PetriNetProcess.Event = {
    case e: io.kagera.akka.persistence.Initialized => deserialize(e)
    case e: io.kagera.akka.persistence.TransitionFired => deserialize(state, e)
    case e: io.kagera.akka.persistence.TransitionFailed => null
  }

  def serializeEvent(state: ExecutionState[S]): PetriNetProcess.Event => AnyRef = {
    case e: InitializedEvent[_] => serialize(e.asInstanceOf[InitializedEvent[S]])
    case e: TransitionFiredEvent => serialize(e)
    case e: TransitionFailedEvent => null
  }

  def persistEvent[T, E <: Event](state: ExecutionState[S], e: E)(fn: (ExecutionState[S], E) => T): Unit = {
    val serializedEvent = serializeEvent(state)(e)
    val updatedState = PetriNetExecution.eventSource(state)(e)
    persist(serializedEvent) { persisted =>
      fn.apply(updatedState, e)
    }
  }

  private var recoveringState: ExecutionState[S] = ExecutionState.uninitialized[S](process)

  override def receiveRecover: Receive = {
    case e: io.kagera.akka.persistence.Initialized =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = PetriNetExecution.eventSource(recoveringState)(deserializedEvent)
    case e: io.kagera.akka.persistence.TransitionFired =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = PetriNetExecution.eventSource(recoveringState)(deserializedEvent)
    case RecoveryCompleted =>
      if (recoveringState.sequenceNr > 0)
        onRecoveryCompleted(recoveringState)
  }
}
