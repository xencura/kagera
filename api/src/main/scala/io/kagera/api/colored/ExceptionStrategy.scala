package io.kagera.api.colored

object ExceptionStrategy {

  /**
   * Indicates a non recoverable exception that should stop any execution of this and other transitions.
   */
  case object Fatal extends ExceptionStrategy

  /**
   * Indicates that this transition should not be retried but others in the process still can.
   */
  case object BlockSelf extends ExceptionStrategy

  /**
   * Indicates that the the firing, using the same input, should be retried with a delay.
   */
  case class RetryWithDelay(initialDelay: Long, maximumRetries: Int, timeFunction: Long => Long = time => time * 2)
      extends ExceptionStrategy

  /**
   * The exception thrown when retry is exhausted.
   *
   * @param nrOfRetries
   */
  case class RetryExhausted(nrOfRetries: Int) extends RuntimeException
}

trait ExceptionStrategy
