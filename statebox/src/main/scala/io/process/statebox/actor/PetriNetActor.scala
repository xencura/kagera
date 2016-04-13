package io.process.statebox.actor

import akka.actor._
import io.process.statebox.actor.PetriNetActor._
import io.process.statebox.actor.PetriNetDebugging.Step
import io.process.statebox.process._
import io.process.statebox.process.simple._

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
  case class FireTransition[T](transition: T)

  sealed trait Event

  case class TransitionFired[T, P, M](transition: T, consumed: M, produced: M, meta: Any) extends Event
  case object NoFireableTransitions extends IllegalStateException
}

class PetriNetActor[T, P, M](process: PTProcess[P, T, M], initialMarking: M)(implicit markingLike: MarkingLike[M, P])
    extends Actor
    with ActorLogging {

  def receive = active(initialMarking)

  def active(marking: M): Receive = {
    case GetState =>
      sender() ! marking
    case FireTransition(t) =>
    // TODO implement
    case Step =>
      stepRandom[P, T, M](process, marking) match {
        case None => sender() ! Status.Failure(NoFireableTransitions)
        case Some((t, consume)) =>
          val produce = process.fireTransition(consume)(t)
          val newMarking = marking.consume(consume).produce(produce)
          sender() ! TransitionFired(t, consume, produce, None)
          log.info("Fired transition {} resulting in marking {}", t, newMarking)
          context become active(newMarking)
      }
  }
}
