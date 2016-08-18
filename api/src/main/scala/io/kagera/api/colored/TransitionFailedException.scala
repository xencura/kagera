package io.kagera.api.colored

class TransitionFailedException(message: String, reason: Throwable) extends RuntimeException(message, reason) {

  def this(t: Transition[_, _, _], reason: Exception) = this(s"Transition $t failed to fire", reason)
}
