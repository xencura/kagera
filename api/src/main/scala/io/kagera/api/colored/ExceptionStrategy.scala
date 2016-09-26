package io.kagera.api.colored

object ExceptionStrategy {

  /**
   * Indicates a non recoverable exception that should prevent any execution of this and other transitions.
   */
  case object Fatal extends ExceptionStrategy

  /**
   * Indicates that this transition should not be retried but others in the process still can.
   */
  case object BlockSelf extends ExceptionStrategy

  /**
   * Retries firing the transition after some delay.
   */
  case class RetryWithDelay(delay: Long) extends ExceptionStrategy {
    require(delay > 0, "Delay must be greater then zero")
  }
}

sealed trait ExceptionStrategy
