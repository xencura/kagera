package io.kagera.execution

import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored.{ Marking, Transition, _ }

/**
 * A Job describes all the parameters that make a firing transition in a petri net.
 */
case class Job[S, T <: Transition[_, _, S]](
  id: Long,
  processState: S,
  transition: T,
  consume: Marking,
  input: Any,
  failure: Option[ExceptionState] = None
) {

  def isActive: Boolean = failure match {
    case Some(ExceptionState(_, _, _, RetryWithDelay(_))) => true
    case None => true
    case _ => false
  }

  lazy val failureCount = failure.map(_.consecutiveFailureCount).getOrElse(0)
}
