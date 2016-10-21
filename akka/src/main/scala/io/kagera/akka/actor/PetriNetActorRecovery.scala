package io.kagera.akka.actor

import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.akka.actor.PetriNetExecution.ExecutionState
import io.kagera.akka.actor.PetriNetProcess.TransitionFiredEvent
import io.kagera.api.colored.{ ExecutablePetriNet, Marking }

import scala.collection.Map

trait PetriNetActorRecovery[S] extends PetriNetEventAdapter[S] {

  this: PersistentActor =>

  override implicit def system = context.system

  def applyEvent(state: ExecutionState[S]): Any => ExecutionState[S] = event =>
    event match {
      case e: TransitionFiredEvent => state.apply(e)
    }

  def initialState: (ExecutablePetriNet[S], Marking, S)
  def onRecoveryCompleted(state: ExecutionState[S])

  private var recoveringState: ExecutionState[S] = null

  def initialRecoveryState = {
    val (process, initialMarking, initialProcessState) = initialState
    ExecutionState[S](process, 1, initialMarking, initialProcessState, Map.empty)
  }

  def recoveredState: ExecutionState[S] = {
    if (recoveringState == null)
      initialRecoveryState
    else
      recoveringState
  }

  override def receiveRecover: Receive = {
    case e: io.kagera.akka.persistence.TransitionFired =>
      if (recoveringState == null)
        recoveringState = initialRecoveryState
      val deserializedEvent = readEvent(recoveringState.process, recoveringState.marking, e)
      recoveringState = recoveringState.apply(deserializedEvent)
    case RecoveryCompleted =>
      onRecoveryCompleted(recoveredState)
  }
}
