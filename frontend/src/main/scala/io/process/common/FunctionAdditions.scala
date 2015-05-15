package io.process.common

object FunctionAdditions {

  def applySideEffect[A, B](fn: A => B, sideEffect: B => Unit): A => B = { input =>
    val result = fn(input)
    sideEffect(result)
    result
  }

  implicit class FunctionWithSideEffect[A, B](fn: A => B) {
    def withSideEffect(sideEffect: B => Unit): A => B = applySideEffect(fn, sideEffect)
  }
}
