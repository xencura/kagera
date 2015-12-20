package io.process.statebox.actor

import akka.actor._
import io.process.statebox.actor.PetriNetActor.NoFireableTransitions
import io.process.statebox.actor.PetriNetDebugging.Step
import io.process.statebox.process.{ PTProcess, PetriNet }

// states
sealed trait ExecutionState
case object Halted extends ExecutionState
case object Active extends ExecutionState

object PetriNetActor {

  def props[P, T, M](process: PTProcess[P, T, M], marking: M) = Props(new PetriNetActor(process, marking))

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  //  case class FireTransition(fn: MarkingHolder => (Transition, MarkingHolder, Any)) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired[T, P, M](transition: T, consumed: M, produced: M, meta: Any) extends Event

  case object NoFireableTransitions extends IllegalStateException
}

class PetriNetActor[T, P, M](process: PTProcess[P, T, M], marking: M) extends Actor with ActorLogging {

  def receive = active(marking)

  def active(currentMarking: M): Receive = { case Step =>
    process.enabledParameters(currentMarking).headOption match {
      case None => sender() ! Status.Failure(NoFireableTransitions)
      case Some((t, markings)) =>
        val newMarking = process.fireTransition(currentMarking)(t, markings.head)
        context become active(newMarking)
        sender() ! "done"
    }
  }
}
