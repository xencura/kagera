package io.process.statebox
package process

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ ActorLogging, ActorRef }
import akka.persistence.PersistentActor
import io.process.{ PTProcess, TProcess }
import io.process.statebox.process.PetriNetActor._

// states
sealed trait ExecutionState
case object Halted extends ExecutionState
case object Active extends ExecutionState

object PetriNetActor {

  //  def apply(petrinet: ProcessModel, id: Long, scheduler: ActorRef) = new PetriNetActor(petrinet, id, scheduler)

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  case class FireTransition(fn: Marking => (Transition, Marking, Any)) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired(transition: Long, consumed: Marking, produced: Marking, meta: Any) extends Event
}

class PetriNetActor[T, P](process: PTProcess[T, P], scheduler: ActorRef) extends PersistentActor with ActorLogging {

  override val persistenceId = s"persistent-process-${context.self.path.name}"

  var marking: Marking = Map.empty
  val seq: AtomicLong = new AtomicLong(0)

  override def receiveCommand = active
  override def receiveRecover = { case e: Event => updateState(e) }

  def active: Receive = {
    case FireTransition(fn) => (fire _ tupled)(fn(marking))
    case GetState => (seq, sender() ! marking)
  }

  def fire(t: Transition, consume: Marking, data: Any) = {

    val initiator = sender()
    //    persist(TransitionFired(t, consume, produced)) { e =>
    //      updateState(e)
    //      initiator ! e
    //    }
  }

  def updateState(e: Event) = e match {
    case TransitionFired(t, consume, produced, meta) =>
      //      marking --= consume.seq
      marking ++= produced
      seq.incrementAndGet()
  }
}
