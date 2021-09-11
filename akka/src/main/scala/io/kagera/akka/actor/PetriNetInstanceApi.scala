package io.kagera.akka.actor

import akka.NotUsed
import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.stream.scaladsl.{ Sink, Source, SourceQueueWithComplete }
import akka.stream.{ Materializer, OverflowStrategy }
import akka.util.Timeout
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored.{ Transition, _ }

import scala.collection.immutable.Seq
import scala.concurrent.{ Await, Future }

sealed trait ErrorResponse {
  def msg: String
}

object ErrorResponse {
  def unapply(arg: ErrorResponse): Option[String] = Some(arg.msg)
}

case class UnexpectedMessage(msg: String) extends ErrorResponse

case object UnknownProcessId extends ErrorResponse {
  val msg: String = s"Unknown process id"
}

/**
 * An actor that pushes all received messages on a SourceQueueWithComplete.
 */
class QueuePushingActor[E](queue: SourceQueueWithComplete[E], takeWhile: Any => Boolean) extends Actor {
  override def receive: Receive = { case msg @ _ =>
    queue.offer(msg.asInstanceOf[E])
    if (!takeWhile(msg)) {
      queue.complete()
      context.stop(self)
    }
  }
}

object PetriNetInstanceApi {

  def hasAutomaticTransitions[S](topology: ExecutablePetriNet[S]): InstanceState[S] => Boolean = state => {
    state.marking.keySet
      .map(p => topology.outgoingTransitions(p))
      .foldLeft(Set.empty[Transition[_, _, _]]) { case (result, transitions) =>
        result ++ transitions
      }
      .exists(isEnabledInState(topology, state))
  }

  def isEnabledInState[S](topology: ExecutablePetriNet[S], state: InstanceState[S])(t: Transition[_, _, _]): Boolean =
    t.isAutomated && !state.hasFailed(t.id) && topology.isEnabledInMarking(state.marking.multiplicities)(t)

  def takeWhileNotFailed[S](topology: ExecutablePetriNet[S], waitForRetries: Boolean): Any => Boolean = {
    case e: TransitionFired[S] => hasAutomaticTransitions(topology)(e.result)
    case TransitionFailed(_, _, _, _, RetryWithDelay(_)) => waitForRetries
    case _ => false
  }

  def askSource[E](actor: ActorRef, msg: Any, takeWhile: Any => Boolean)(implicit
    actorSystem: ActorSystem
  ): Source[E, NotUsed] = {
    Source.queue[E](100, OverflowStrategy.fail).mapMaterializedValue { queue =>
      val sender = actorSystem.actorOf(Props(new QueuePushingActor[E](queue, takeWhile)))
      actor.tell(msg, sender)
      NotUsed.getInstance()
    }
  }
}

/**
 * Contains some methods to interact with a petri net instance actor.
 */
class PetriNetInstanceApi[S](topology: ExecutablePetriNet[S], actor: ActorRef)(implicit
  actorSystem: ActorSystem,
  materializer: Materializer
) {

  import PetriNetInstanceApi._
  import actorSystem.dispatcher

  /**
   * Fires a transition and confirms (waits) for the result of that transition firing.
   */
  def askAndConfirmFirst(msg: Any)(implicit timeout: Timeout): Future[Either[UnexpectedMessage, InstanceState[S]]] = {
    actor.ask(msg).map {
      case e: TransitionFired[_] => Right(e.result.asInstanceOf[InstanceState[S]])
      case msg @ _ => Left(UnexpectedMessage(s"Received unexepected message: $msg"))
    }
  }

  def askAndConfirmFirstSync(msg: Any)(implicit timeout: Timeout): Either[UnexpectedMessage, InstanceState[S]] = {
    Await.result(askAndConfirmFirst(topology, msg), timeout.duration)
  }

  /**
   * Fires a transition and confirms (waits) for all responses of subsequent automated transitions.
   */
  def askAndConfirmAll(msg: Any, waitForRetries: Boolean = false)(implicit
    timeout: Timeout
  ): Future[Either[ErrorResponse, InstanceState[S]]] = {

    val futureMessages = askAndCollectAll(msg, waitForRetries).runWith(Sink.seq)

    futureMessages.map {
      _.lastOption match {
        case Some(e: TransitionFired[_]) => Right(e.result.asInstanceOf[InstanceState[S]])
        case Some(msg) => Left(UnexpectedMessage(s"Received unexpected message: $msg"))
        case None => Left(UnknownProcessId)
      }
    }
  }

  /**
   * Synchronously collects all messages in response to a message sent to a PetriNet instance.
   */
  def askAndCollectAllSync(msg: Any, waitForRetries: Boolean = false)(implicit
    timeout: Timeout
  ): Seq[TransitionResponse] = {
    val futureResult = askAndCollectAll(msg, waitForRetries).runWith(Sink.seq)
    Await.result(futureResult, timeout.duration)
  }

  /**
   * Sends a FireTransition command to the actor and returns a Source of TransitionResponse messages
   */
  def fireTransition(transitionId: Long, input: Any): Source[TransitionResponse, NotUsed] =
    askAndCollectAll(FireTransition(transitionId, input))

  /**
   * Returns a Source of all the messages from a petri net actor in reponse to a message.
   *
   * If the instance is 'uninitialized' returns an empty source.
   */
  def askAndCollectAll(msg: Any, waitForRetries: Boolean = false): Source[TransitionResponse, NotUsed] = {
    askSource[Any](actor, msg, takeWhileNotFailed(topology, waitForRetries))
      .map {
        case e: TransitionResponse => Right(e)
        case msg @ _ => Left(s"Received unexpected message: $msg")
      }
      .takeWhile(_.isRight)
      .collect { case Right(r) => r }
  }
}
