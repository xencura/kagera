package io.kagera.api.colored

import io.kagera.api.colored.ExceptionStrategy.BlockSelf

import scala.concurrent.duration.Duration

abstract class AbstractTransition[I, O, S](
  override val id: Long,
  override val label: String,
  override val isAutomated: Boolean,
  override val maximumOperationTime: Duration = Duration.Undefined,
  override val exceptionStrategy: TransitionExceptionHandler = (e, n) => BlockSelf
) extends Transition[I, O, S] {}
