package io.kagera.execution

import io.kagera.api.colored.{ Marking, Transition, _ }

case class Job[S, E](
  id: Long,
  processState: S,
  transition: Transition[Any, E, S],
  consume: Marking,
  input: Any,
  failure: Option[ExceptionState] = None
) {

  lazy val failureCount = failure.map(_.consecutiveFailureCount).getOrElse(0)
}
