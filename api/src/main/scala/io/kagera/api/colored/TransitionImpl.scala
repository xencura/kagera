package io.kagera.api.colored

abstract class TransitionImpl[I, O](override val id: Long, override val label: String, override val isManaged: Boolean)
    extends Transition {

  override def toString = label
  type Input = I
  type Output = O
}
