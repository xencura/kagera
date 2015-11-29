package io.process.statebox.actor

import akka.actor._
import io.process.statebox.actor.PetriNetActor.NoFireableTransitions
import io.process.statebox.actor.PetriNetDebugging.Step
import io.process.statebox.process.PetriNet
import io.process.statebox.process.simple._

// states
sealed trait ExecutionState
case object Halted extends ExecutionState
case object Active extends ExecutionState

object PetriNetActor {

  def props[P, T](process: PetriNet[P, T]) = Props(new PetriNetActor(process))

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  //  case class FireTransition(fn: MarkingHolder => (Transition, MarkingHolder, Any)) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired[P](transition: Long, consumed: Marking[P], produced: Marking[P], meta: Any) extends Event

  case object NoFireableTransitions extends IllegalStateException
}

class PetriNetActor[T, P](process: PetriNet[P, T]) extends Actor with ActorLogging {

  def receive = active(Map.empty)

  def active(marking: Marking[P]): Receive = { case Step =>
    process.enabledTransitions(marking).headOption match {
      case None => sender() ! Status.Failure(NoFireableTransitions)
      case Some(t) =>
        val newMarking = fireTransition(marking, t)
        context become active(newMarking)
        sender() ! "done"
    }
  }

  def fireTransition(marking: Marking[P], t: T): Marking[P] = {

    log.debug("Firing transition {}", t)

    val in = process.inMarking(t)
    log.debug("inMarking: {}", in)

    val out = process.outMarking(t)
    log.debug("outMarking: {}", out)

    val newMarking = marking.consume(in).produce(out)

    log.info("fired transition {}, result: {}", t, newMarking)

    newMarking
  }
}
