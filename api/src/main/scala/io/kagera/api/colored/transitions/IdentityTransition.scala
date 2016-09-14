package io.kagera.api.colored.transitions

import io.kagera.api.colored.Transition

trait IdentityTransition[Input, Output, State] extends Transition[Input, Output, State] {

  override def updateState: State => Output => State = s => e => s
}
