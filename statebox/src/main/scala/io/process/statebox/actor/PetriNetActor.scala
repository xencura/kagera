package io.process.statebox.actor

import akka.actor._
import io.process.statebox.actor.PetriNetActor.{ NoFireableTransitions, TransitionFired }
import io.process.statebox.actor.PetriNetDebugging.Step
import io.process.statebox.process._

// states
sealed trait ExecutionState
case object Halted extends ExecutionState
case object Active extends ExecutionState

object PetriNetActor {

  def props[P, T, M](process: PTProcess[P, T, M], marking: M)(implicit markingLike: MarkingLike[M, P]) = Props(
    new PetriNetActor(process, marking)
  )

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  //  case class FireTransition(fn: MarkingHolder => (Transition, MarkingHolder, Any)) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired[T, P, M](transition: T, consumed: M, produced: M, meta: Any) extends Event

  case object NoFireableTransitions extends IllegalStateException
}

class PetriNetActor[T, P, M](process: PTProcess[P, T, M], initialMarking: M)(implicit markingLike: MarkingLike[M, P])
    extends Actor
    with ActorLogging {

  def receive = active(initialMarking)

  def active(marking: M): Receive = { case Step =>
    process.enabledParameters(marking).headOption match {
      case None => sender() ! Status.Failure(NoFireableTransitions)
      case Some((t, enabledMarkings)) =>
        val consume = enabledMarkings.head
        val produce = process.fireTransition(consume)(t)
        val newMarking = marking.consume(consume).produce(produce)
        sender() ! TransitionFired(t, consume, produce, None)
        log.info("Fired transition {} resulting in marking {}", t, newMarking)
        context become active(newMarking)
    }
  }
}
