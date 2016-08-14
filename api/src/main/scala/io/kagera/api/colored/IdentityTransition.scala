package io.kagera.api.colored

import scala.concurrent.duration.Duration

abstract class IdentityTransition[I, E, S](id: Long, label: String, isManaged: Boolean, maximumOperationTime: Duration)
    extends AbstractTransition[I, E, S](id, label, isManaged, maximumOperationTime) {

  override def updateState(s: S): (E) => S = e => s
}
