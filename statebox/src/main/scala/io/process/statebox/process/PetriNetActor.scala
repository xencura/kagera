package io.process.statebox
package process

import akka.actor._
import io.process.statebox.process.PetriNetActor._
import io.process.statebox.process.PetriNetDebugging.Step
import io.process.statebox.process.dsl._

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
    consumed: SimpleMarking[Place],
    produced: SimpleMarking[Place],
    meta: Any
  ) extends Event

  case object NoFireableTransitions extends IllegalStateException

}

class PetriNetActor[T, P](process: ColoredPetriNet) extends Actor with ActorLogging {

  def receive = active(Map.empty)

  def active(marking: SimpleMarking[Place]): Receive = { case Step =>
    process.enabledTransitions(marking).headOption match {
      case None => sender() ! Status.Failure(NoFireableTransitions)
      case Some(t) => fire(marking, t)
    }
  }

  def fire(marking: SimpleMarking[Place], t: Transition) = {

    log.warning(s"Firing transition $t")

    val in = process.inMarking(t)
    log.warning(s"inMarking: $in")

    val out = process.outMarking(t)
    log.warning(s"outMarking: $out")

    val newMarking = marking.consume(in).produce(out)

    log.warning(s"result: $marking")

    context become active(newMarking)

    sender() ! "fired"
  }
}
