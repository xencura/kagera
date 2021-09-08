package io.kagera.api.colored.dsl

import cats.Applicative
import cats.effect.IO
import io.kagera.api.colored.ExceptionStrategy.BlockTransition
import io.kagera.api.colored.transitions.{ AbstractTransition, UncoloredTransitionExecutorFactory }
import io.kagera.api.colored.{ Marking, Transition, _ }

import scala.concurrent.duration.Duration
import scala.util.Random

trait StateTransitionNet[S, E] {

  def eventSourcing: S => E => S

  def transition(
    id: Long = Math.abs(Random.nextLong),
    label: Option[String] = None,
    automated: Boolean = false,
    exceptionStrategy: TransitionExceptionHandler = (e, n) => BlockTransition
  )(fn: S => E): StateTransitionNetTransition[S, E] =
    new StateTransitionNetTransition[S, E](
      id,
      label.getOrElse(s"t$id"),
      automated,
      exceptionStrategy,
      eventSourcing,
      fn
    )
  def createPetriNet(arcs: Arc[StateTransitionNetTransition[S, E]]*) =
    process[S, StateTransitionNetTransition[S, E]](arcs: _*)
}

class StateTransitionNetTransition[S, E](
  id: Long,
  label: String,
  automated: Boolean,
  exceptionStrategy: TransitionExceptionHandler,
  val eventSourcing: S => E => S,
  val transitionFunction: S => E
) extends AbstractTransition[Unit, E, S](id, label, automated, Duration.Undefined, exceptionStrategy)
    with Transition[Unit, E, S] {
  override val toString = label
  override val updateState = eventSourcing
}
class StateTransitionNetExecutorFactory[S, E]
    extends UncoloredTransitionExecutorFactory[cats.Id, StateTransitionNetTransition[S, E]] {
  type Input = Unit
  type Output = E
  type State = S
  override def produceEvent(t: StateTransitionNetTransition[S, E], consume: Marking, state: S, input: Unit): E =
    t.transitionFunction(state)
}
