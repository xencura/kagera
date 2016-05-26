package io.kagera.akka.actor

import akka.actor._
import io.kagera.akka.actor.PetriNetActor._
import io.kagera.akka.actor.PetriNetDebugging.Step
import io.kagera.api._
import io.kagera.api.colored._

object PetriNetActor {

  def props(id: java.util.UUID, process: ColoredTokenGame, initialMarking: ColoredMarking) =
    Props(new PetriNetActor(id, process, initialMarking))

  sealed trait Command

  case object GetState extends Command
  case class FireTransition(transition: Transition, data: Option[Any] = None)

  sealed trait Event

  case class TransitionFailed(t: Transition, reason: Exception) extends RuntimeException
  case class TransitionFired(t: Transition, consumed: ColoredMarking, produced: ColoredMarking, meta: Any) extends Event
  case object NoFireableTransitions extends IllegalStateException
}

class PetriNetActor(id: java.util.UUID, process: ColoredTokenGame, initialMarking: ColoredMarking)
    extends Actor
    with ActorLogging {

  import context.dispatcher

  def receive = active(initialMarking)

  def active(marking: ColoredMarking): Receive = {
    case GetState =>
      sender() ! marking
    case Step =>
      TokenGame.stepRandom[Place, Transition, ColoredMarking](process, marking) match {
        case None => sender() ! Status.Failure(NoFireableTransitions)
        case Some((consume, t)) =>
          val produce =
            ColoredExecutor
              .executeTransition(process)(consume, t, None, id)
              .map(produce => marking.consume(consume).produce(produce))

        //          log.info("Fired transition {} resulting in marking {}", t, newMarking)
        //          context become active(newMarking)
      }

    case FireTransition(t, data) =>
  }
}
