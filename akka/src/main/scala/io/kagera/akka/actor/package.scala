package io.kagera.akka

import io.kagera.api._
import io.kagera.api.colored._

package object actor {
  implicit class ProcessFns[S](process: ColoredPetriNetProcess[S]) {

    def getTransitionById(id: Long): Transition[Any, Any, S] =
      process.transitions.getById(id).asInstanceOf[Transition[Any, Any, S]]
  }
}
