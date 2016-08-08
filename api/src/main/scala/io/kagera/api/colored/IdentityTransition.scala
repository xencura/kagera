package io.kagera.api.colored

abstract class IdentityTransition[T](id: Long, label: String, isManaged: Boolean)
    extends AbstractTransition[T, T](id, label, isManaged) {

  override def updateState(e: T): (Context) => Context = s => s
}
