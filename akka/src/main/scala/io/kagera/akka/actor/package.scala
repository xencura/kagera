package io.kagera.akka

import io.kagera.api.colored.{ Transition, _ }

/**
 * Created by su43xu on 23-8-2016.
 */
package object actor {
  implicit class ProcessFns[S](process: ColoredPetriNetProcess[S]) {

    def getTransitionById(id: Long): Transition[Any, Any, S] =
      process.transitions.getById(id).asInstanceOf[Transition[Any, Any, S]]
  }
}
