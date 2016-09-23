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
   */
  case class RetryWithDelay(delay: Long) extends ExceptionStrategy

  /**
   * The exception thrown when retry is exhausted.
   *
   * @param nrOfRetries
   */
  case class RetryExhausted(nrOfRetries: Int) extends RuntimeException
}

sealed trait ExceptionStrategy
