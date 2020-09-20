package io.kagera.api.colored.transitions

import io.kagera.api.colored.ExceptionStrategy.BlockTransition
import io.kagera.api.colored.{ Transition, _ }

import scala.concurrent.duration.Duration

abstract class AbstractTransition[I, O, S](
  override val id: Long,
  override val label: String,
  override val isAutomated: Boolean,
  override val maximumOperationTime: Duration = Duration.Undefined,
  override val exceptionStrategy: TransitionExceptionHandler = (e, n) => BlockTransition
) extends Transition[I, O, S] {}
