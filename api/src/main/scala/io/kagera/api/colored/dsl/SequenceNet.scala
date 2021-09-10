package io.kagera.api.colored.dsl

import cats.Applicative
import cats.effect.IO
import io.kagera.api.colored.ExceptionStrategy.BlockTransition
import io.kagera.api.colored._
import io.kagera.api.colored.transitions.{ AbstractTransition, UncoloredTransitionExecutorFactory }

import scala.concurrent.duration.Duration

case class TransitionBehaviour[S, E](automated: Boolean, exceptionHandler: TransitionExceptionHandler, fn: S => E) {
  def asTransition(id: Long, eventSource: S => E => S) =
    new SequenceNetTransition[S, E](id, automated, exceptionHandler, eventSource, fn)
}

class SequenceNetTransition[S, E](
  id: Long,
  automated: Boolean,
  exceptionHandler: TransitionExceptionHandler,
  eventSource: S => E => S,
  val fn: S => E
) extends AbstractTransition[Any, E, S](id, s"t$id", automated, Duration.Undefined, exceptionHandler) {
  override val toString = label
  override val updateState = eventSource
}

class SequenceNetTransitionExecutorFactory[F[_] : Applicative, S, E]
    extends UncoloredTransitionExecutorFactory[F, SequenceNetTransition[S, E]] {
  type Input = Any
  type Output = E
  type State = S
  override def produceEvent(t: SequenceNetTransition[S, E], consume: Marking, state: S, input: Any): F[E] =
    Applicative.apply.pure(t.fn(state))
}

object SequenceNetTransitionExecutorFactory {
  implicit def sequenceNetTransitionExecutorFactory[F[_] : Applicative, S, E]
    : TransitionExecutorFactory[F, SequenceNetTransition[S, E]] = new SequenceNetTransitionExecutorFactory[F, S, E]
}

trait SequenceNet[S, E] {

  def sequence: Seq[TransitionBehaviour[S, E]]

  def eventSourcing: S => E => S

  lazy val places = (1 to (sequence.size + 1)).map(i => Place[Unit](id = i))
  lazy val transitions = petriNet.transitions
  lazy val initialMarking = Marking(place(1) -> 1)

  def place(n: Int) = places(n - 1)

  def transition(automated: Boolean = false, exceptionHandler: TransitionExceptionHandler = (e, n) => BlockTransition)(
    fn: S => E
  ): TransitionBehaviour[S, E] = TransitionBehaviour(automated, exceptionHandler, fn)

  lazy val petriNet = {
    val nrOfSteps = sequence.size
    val transitions = sequence.zipWithIndex.map { case (t, index) => t.asTransition(index + 1, eventSourcing) }

    val places = (1 to (nrOfSteps + 1)).map(i => Place[Unit](id = i))
    val tpedges = transitions.zip(places.tail).map { case (t, p) => arc(t, p, 1) }
    val ptedges = places.zip(transitions).map { case (p, t) => arc(p, t, 1) }
    process[S, SequenceNetTransition[S, E]]((tpedges ++ ptedges): _*)
  }
}
