package io.process.statebox
package process

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ Actor, ActorLogging, Status }
import io.process.statebox.process.PetriNetActor._
import io.process.statebox.process.PetriNetDebugging.Step
import io.process.statebox.process.dsl._

// states
sealed trait ExecutionState
case object Halted extends ExecutionState
case object Active extends ExecutionState

object PetriNetActor {

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  //  case class FireTransition(fn: MarkingHolder => (Transition, MarkingHolder, Any)) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired(transition: Long, consumed: SimpleMarking, produced: SimpleMarking, meta: Any)
      extends Event

  case object NoFireableTransitions extends IllegalStateException

}

class PetriNetActor[T, P](id: String, process: ColoredPetriNet) extends Actor with ActorLogging {

  var marking: SimpleMarking = Map.empty
  val seq: AtomicLong = new AtomicLong(0)

  def receive: Receive = { case Step =>
    process.enabledTransitions(marking).headOption match {
      case None => sender() ! Status.Failure(NoFireableTransitions)
      case Some(t) => fire(t)
    }
  }

  def fire(t: Transition) = {

    log.warning(s"Firing transition $t")

    val in = process.inMarking(t)
    log.warning(s"inMarking: $in")

    val out = process.outMarking(t)
    log.warning(s"outMarking: $out")

    marking = marking.consume(in).produce(out)

    log.warning(s"result: $marking")

    seq.incrementAndGet()

    sender() ! "fired"
  }
}
