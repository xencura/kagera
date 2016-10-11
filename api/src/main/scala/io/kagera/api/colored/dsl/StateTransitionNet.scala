package io.kagera.api.colored.dsl

import io.kagera.api.colored.ExceptionStrategy.BlockSelf
import io.kagera.api.colored.{ AbstractTransition, Marking, Transition, _ }
import io.kagera.api.colored.transitions.UncoloredTransition

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration
import scala.util.Random

trait StateTransitionNet[S, E] {

  def eventSourcing: S => E => S

  def transition(
    id: Long = Math.abs(Random.nextLong),
    label: Option[String] = None,
    automated: Boolean = false,
    exceptionStrategy: TransitionExceptionHandler = (e, n) => BlockSelf
  )(fn: S => E): Transition[Unit, E, S] =
    new AbstractTransition[Unit, E, S](id, label.getOrElse(s"t$id"), automated, Duration.Undefined, exceptionStrategy)
      with UncoloredTransition[Unit, E, S] {
      override val toString = label
      override val updateState = eventSourcing
      override def produceEvent(consume: Marking, state: S, input: Unit)(implicit
        executor: ExecutionContext
      ): Future[E] = Future { (fn(state)) }
    }

  def createPetriNet(arcs: Arc*)(implicit ec: ExecutionContext) = process[S](arcs: _*)
}
