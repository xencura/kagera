package io.kagera.akka.actor

import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.execution.Instance
import io.kagera.execution.EventSourcing._
import io.kagera.persistence.EventSerializer

trait PetriNetInstanceRecovery[S] extends EventSerializer[S] with AkkaObjectSerializer {

  this: PersistentActor =>

  def process: ExecutablePetriNet[S]

  override implicit def system = context.system

  def onRecoveryCompleted(state: Instance[S])

  def updateInstance(i: Instance[S])(e: Event): Instance[S] = applyEvent(e).runS(i).value

  def persistEvent[T, E <: Event](instance: Instance[S], e: E)(fn: (Instance[S], E) => T): Unit = {
    val serializedEvent = serializeEvent(instance)(e)
    val updatedState = updateInstance(instance)(e)
    persist(serializedEvent) { persisted =>
      fn.apply(updatedState, e)
    }
  }

  private var recoveringState: Instance[S] = Instance.uninitialized[S](process)

  override def receiveRecover: Receive = {
    case e: io.kagera.persistence.Initialized =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = updateInstance(recoveringState)(deserializedEvent)
    case e: io.kagera.persistence.TransitionFired =>
      val deserializedEvent = deserializeEvent(recoveringState)(e)
      recoveringState = updateInstance(recoveringState)(deserializedEvent)
    case RecoveryCompleted =>
      if (recoveringState.sequenceNr > 0)
        onRecoveryCompleted(recoveringState)
  }
}
