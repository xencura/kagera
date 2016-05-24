package io.kagera.api.colored

import scala.concurrent.Future

abstract class IdentityTransition[T](id: Long, label: String, isManaged: Boolean)
    extends AbstractTransition[T, T](id, label, isManaged) {

  override def apply(input: T)(implicit executor: scala.concurrent.ExecutionContext): Future[T] =
    Future.successful(input)
}
