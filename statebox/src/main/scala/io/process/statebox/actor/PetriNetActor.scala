package io.process.statebox.actor

import akka.actor._
import io.process.statebox.actor.PetriNetActor.NoFireableTransitions
import io.process.statebox.actor.PetriNetDebugging.Step
import io.process.statebox.process.colored._
import io.process.statebox.process.simple._

// states
sealed trait ExecutionState
case object Halted extends ExecutionState
case object Active extends ExecutionState

object PetriNetActor {

  def props(process: ColoredPetriNet) = Props(new PetriNetActor(process))

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  //  case class FireTransition(fn: MarkingHolder => (Transition, MarkingHolder, Any)) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired(
    transition: Long,
    consumed: Marking[ColoredPlace],
    produced: Marking[ColoredPlace],
    meta: Any
  ) extends Event

  case object NoFireableTransitions extends IllegalStateException

}

class PetriNetActor[T, P](process: ColoredPetriNet) extends Actor with ActorLogging {

  def receive = active(Map.empty)

  def active(marking: Marking[ColoredPlace]): Receive = { case Step =>
    process.enabledTransitions(marking).headOption match {
      case None => sender() ! Status.Failure(NoFireableTransitions)
      case Some(t) =>
        val newMarking = fireTransition(marking, t)
        context become active(newMarking)
    }
  }

  def fireTransition(marking: Marking[ColoredPlace], t: Transition): Marking[ColoredPlace] = {

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
