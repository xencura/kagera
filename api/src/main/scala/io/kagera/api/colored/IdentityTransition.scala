package io.kagera.api.colored

import scala.concurrent.duration.Duration

abstract class IdentityTransition[I, O, S](id: Long, label: String, isManaged: Boolean, maximumOperationTime: Duration)
    extends AbstractTransition[I, O, S](id, label, isManaged, maximumOperationTime) {

  override def updateState(e: O): (S) => S = s => s
}
