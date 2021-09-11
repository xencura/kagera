package io.kagera.api.colored.dsl

import cats.effect.Sync
import io.kagera.api.colored.ExceptionStrategy.BlockTransition
import io.kagera.api.colored.transitions.{ AbstractTransition, UncoloredTransition }
import io.kagera.api.colored.{ Marking, Transition, _ }

import scala.concurrent.duration.Duration
import scala.util.Random

trait StateTransitionNet[S, E] {

  def eventSourcing: S => E => S

  def transition(
    id: Long = Math.abs(Random.nextLong()),
    label: Option[String] = None,
    automated: Boolean = false,
    exceptionStrategy: TransitionExceptionHandler = (e, n) => BlockTransition
  )(fn: S => E): Transition[Unit, E, S] =
    new AbstractTransition[Unit, E, S](id, label.getOrElse(s"t$id"), automated, Duration.Undefined, exceptionStrategy)
      with UncoloredTransition[Unit, E, S] {
      override val toString = label
      override val updateState = eventSourcing
      override def produceEvent[F[_]](consume: Marking, state: S, input: Unit)(implicit sync: Sync[F]): F[E] =
        Sync.apply.delay { (fn(state)) }
    }

  def createPetriNet(arcs: Arc*) = process[S](arcs: _*)
}
