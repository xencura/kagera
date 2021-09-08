package io.kagera.execution

import io.kagera.api.colored.ExceptionStrategy

case class ExceptionState(
  transitionId: Long,
  consecutiveFailureCount: Int,
  failureReason: String,
  failureStrategy: ExceptionStrategy
)
