package io.kagera.api.colored.dsl

import fs2.Task
import io.kagera.api.colored.ExceptionStrategy.BlockSelf
import io.kagera.api.colored._
import io.kagera.api.colored.transitions.{ AbstractTransition, UncoloredTransition }

import scala.concurrent.duration.Duration

case class TransitionBehaviour[S, E](automated: Boolean, exceptionHandler: TransitionExceptionHandler, fn: S => E) {
  def asTransition(id: Long, eventSource: S => E => S) =
    new AbstractTransition[Unit, E, S](id, s"t$id", automated, Duration.Undefined, exceptionHandler)
      with UncoloredTransition[Unit, E, S] {
      override val toString = label
      override val updateState = eventSource
      override def produceEvent(consume: Marking, state: S, input: Unit): Task[E] = Task.delay { (fn(state)) }
    }
}

trait SequenceNet[S, E] {

  val nrOfSteps = 2

  def eventSourcing: S => E => S

  val places = (1 to nrOfSteps).map(i => Place[Unit](id = i))
  def place(n: Int) = places(n - 1)

  val initialMarking = Marking(place(1) -> 1)

  def sequence: PartialFunction[Long, TransitionBehaviour[S, E]]

  def transition(automated: Boolean = false, exceptionHandler: TransitionExceptionHandler = (e, n) => BlockSelf)(
    fn: S => E
  ): TransitionBehaviour[S, E] = TransitionBehaviour(automated, exceptionHandler, fn)

  lazy val petriNet = {
    val tr = (1 to nrOfSteps)
      .map(i =>
        sequence
          .lift(i)
          .map(_.asTransition(i, eventSourcing))
          .getOrElse(nullTransition(i))
      )

    val places = (1 to (nrOfSteps + 1)).map(i => Place[Unit](id = i))
    val tpedges = tr.zip(places.tail).map { case (t, p) => arc(t, p, 1) }
    val ptedges = places.zip(tr).map { case (p, t) => arc(p, t, 1) }
    process[S]((tpedges ++ ptedges): _*)
  }
}
