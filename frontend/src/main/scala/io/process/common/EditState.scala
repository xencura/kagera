package io.process.common

import io.process.common.FunctionAdditions._

trait EditState[S] {

  // TODO use scalaz's state monad?

  def initialState: S
  private var internalState: S = initialState

  //  def foo:State[S]
  def state: S = internalState

  def onChange(from: S, to: S): Unit = ()

  def apply(fn: S => S) = {
    val from = state
    internalState = fn.withSideEffect(onChange _ curried (from))(internalState)
  }
}
