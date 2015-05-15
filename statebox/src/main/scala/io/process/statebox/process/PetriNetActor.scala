package io.process.statebox.process

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorRef
import akka.persistence.PersistentActor
import io.process.statebox.{ Marking, Token }
import io.process.statebox.process.PetriNetActor._

import scala.concurrent.Future

// states
sealed trait State
case object Halted extends State
case object Active extends State

object PetriNetActor {

  def apply(petrinet: ProcessModel, id: Long, scheduler: ActorRef) = new PetriNetActor(petrinet, id, scheduler)

  sealed trait Command

  case object GetState extends Command
  case object Start extends Command
  case object Step extends Command
  case class FireTransition(transition: Long, tokens: Set[Token] = Set.empty, data: Any = None) extends Command
  case object Stop extends Command

  sealed trait Event

  case class TransitionFired(transition: Long, consumed: Set[Token], produced: Marking)
}

/**
 * An instance of a petri net
 */
class PetriNetActor(petrinet: ProcessModel, id: Long, scheduler: ActorRef) extends PersistentActor {

  override val persistenceId = id.toString

  var unclaimed: Set[Token] = Set.empty
  var claimed: Set[Token] = Set.empty

  def marking = claimed ++ unclaimed

  var nextId: AtomicLong = new AtomicLong(0)
  val executionState: State = Active

  override def receiveCommand = active

  def active: Receive = {

    case Step =>
      val (transition, tokens) = enabled().head
      fire(transition, tokens.head, None)

    case FireTransition(t, consume, data) =>
      fire(t, consume, data)
    case GetState =>
      sender() ! marking
  }

  def validate(t: Long, consume: Set[Token], data: Any) = false

  def enabled(): Map[Long, Seq[Set[Token]]] = {
    Map.empty
  }

  def postFire(t: Long, consume: Marking, produced: Marking): Unit = {}

  def fire(t: Long, consume: Set[Token], data: Any) = {

    val initiator = sender()
    val tfn: Set[Token] => Marking = s => Map.empty

    Future {
      tfn.apply(consume)
    }.map { produced =>
      claimed --= consume
      persist(TransitionFired(t, consume, produced)) { e =>
        scheduler ! e
        initiator ! e
      }
    }
  }

  override def receiveRecover: Receive = ???
}
