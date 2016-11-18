package io.kagera.akka.actor

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue, TimeUnit }

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout
import cats.data.Xor
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored.{ Transition, _ }

import scala.concurrent.Future

import Xor._

/**
 * Contains some methods to interact with a petri net instance actor.
 */
object PetriNetInstanceInteractions {

  case class Error(msg: String)

  /**
   * An actor that pushes all received messages on a blocking queue.
   */
  class QueuePushingActor[E](queue: BlockingQueue[E], takeWhile: Any => Boolean) extends Actor {
    override def receive: Receive = { case msg @ _ =>
      queue.add(msg.asInstanceOf[E])
      if (!takeWhile(msg))
        context.stop(self)
    }
  }

  implicit class IteratorExtension[A](i: Iterator[A]) {
    def takeWhileInclusive(p: A => Boolean): Iterator[A] = {
      val (a, b) = i.span(p)
      a ++ b.take(1)
    }
  }

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

  def takeWhileNotFailed[S](topology: ExecutablePetriNet[S], waitForRetries: Boolean): Any => Boolean = e =>
    e match {
      case e: TransitionFired[S] => hasAutomaticTransitions(topology)(e.result)
      case TransitionFailed(_, _, _, _, RetryWithDelay(delay)) => waitForRetries
      case msg @ _ => false
    }

  implicit class ActorRefAdditions(actor: ActorRef)(implicit actorSystem: ActorSystem) {

    import actorSystem.dispatcher

    def responseIterator[E](msg: Any, takeWhile: Any => Boolean)(implicit timeout: Timeout): Iterator[E] = {
      val queue = new LinkedBlockingQueue[E]()
      val askingActor = actorSystem.actorOf(Props(new QueuePushingActor[E](queue, takeWhile)))
      actor.tell(msg, askingActor)
      Iterator.continually(queue.poll(timeout.duration.toMillis, TimeUnit.MILLISECONDS)).takeWhileInclusive(takeWhile)
    }

    /**
     * Fires a transition and confirms (waits) for the result of that transition firing.
     */
    def fireAndConfirmFirst[S](topology: ExecutablePetriNet[S], msg: Any)(implicit
      timeout: Timeout
    ): Future[Xor[Error, S]] = {
      actor.ask(msg).map {
        case e: TransitionFired[_] => right(e.result.state.asInstanceOf[S])
        case msg @ _ => left(Error(s"Received unexepected message: $msg"))
      }
    }

    /**
     * Fires a transition and confirms (waits) for all responses of subsequent automated transitions.
     */
    def fireAndConfirmAll[S](topology: ExecutablePetriNet[S], msg: Any, waitForRetries: Boolean = false)(implicit
      timeout: Timeout
    ): Future[Xor[Error, S]] = {
      Future {
        val lastResponse = fireAndCollectResponses(topology, msg, waitForRetries).last

        lastResponse match {
          case e: TransitionFired[_] => right(e.result.state.asInstanceOf[S])
          case msg @ _ => left(Error("Transition failed!"))
        }
      }
    }

    /**
     * Collects all the messages from the petri net actor in reponse to a message
     */
    def fireAndCollectResponses[S](topology: ExecutablePetriNet[S], msg: Any, waitForRetries: Boolean = false)(implicit
      timeout: Timeout
    ): Seq[TransitionResponse] = {
      responseIterator[Any](msg, takeWhileNotFailed(topology, waitForRetries)).map {
        case e: TransitionResponse => e
        case msg @ _ => throw new RuntimeException(s"Unexepected message: $msg")
      }.toSeq
    }
  }
}
