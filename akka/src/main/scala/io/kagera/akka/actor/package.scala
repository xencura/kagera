package io.kagera.akka

import io.kagera.api._
import io.kagera.api.colored._

package object actor {
  implicit class ProcessFns[S](process: ExecutablePetriNet[S]) {

    def findTransitionById(id: Long): Option[Transition[Any, Any, S]] =
      process.transitions.findById(id).asInstanceOf[Option[Transition[Any, Any, S]]]

    def getTransitionById(id: Long): Transition[Any, Any, S] = findTransitionById(id).get
  }
}
