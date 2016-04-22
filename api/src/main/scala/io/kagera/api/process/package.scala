package io.kagera.api

import scala.concurrent.Future

package object process {

  type Step[S] = S => Future[S]

  type FlowControl[S, T <: Step[S]] = S => Set[T]

  type FlowExecutor[S, T <: Step[S]] = FlowControl[S, T] => T
}
