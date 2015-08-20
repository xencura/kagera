package io.process.statebox
package process

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ ActorLogging, ActorRef }
import akka.persistence.PersistentActor
import io.process.statebox.process.PetriNetActor._

// states
sealed trait ExecutionState
case object Halted extends ExecutionState
case object Active extends ExecutionState

object PetriNetActor {

  def apply(petrinet: ProcessModel, id: Long, scheduler: ActorRef) = new PetriNetActor(petrinet, id, scheduler)

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  case class FireTransition(transition: Long, tokens: Set[Token] = Set.empty, data: Any = None) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired(transition: Long, consumed: Set[Token], produced: Marking) extends Event
}

/**
 * An instance of a petri net
 *
 * design
 *
 *   - a token id is unique for the lifetime of a process instance
 *   - a token that is not consumed retains it's id in the next marking
 */
class PetriNetActor(petrinet: ProcessModel, id: Long, scheduler: ActorRef) extends PersistentActor with ActorLogging {

  override val persistenceId = id.toString

  var marking: Set[Token] = Set.empty
  var seq: AtomicLong = new AtomicLong(0)
  val executionState: ExecutionState = Active

  override def receiveCommand = active

  override def receiveRecover = { case e: Event => updateState(e) }

  def active: Receive = {
    case FireTransition(t, consume, data) => fire(t, consume, data)
    case GetState => sender() ! marking
  }

  def fire(t: Long, consume: Set[Token], data: Any) = {

    val initiator = sender()
    val tfn: Marking => Marking = s => s
    val produced = tfn.apply(consume)

    persist(TransitionFired(t, consume, produced)) { e =>
      updateState(e)
      initiator ! e
    }
  }

  def updateState(e: Event) = e match {
    case TransitionFired(t, consume, produced) =>
      marking --= consume
      marking ++= produced
      seq.incrementAndGet()

  }
}
