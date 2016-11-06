package io.kagera.api.colored

import fs2.Task
import io.kagera.api._

import scala.concurrent.{ ExecutionContext, Future }

trait TransitionExecutor[S] {

  this: PetriNet[Place[_], Transition[_, _, _]] =>

  val transitionFunctions: Map[Transition[_, _, _], _] =
    transitions.map(t => t -> t.apply(inMarking(t), outMarking(t))).toMap

  def tfn[Input, Output](t: Transition[Input, Output, S]): (Marking, S, Input) => Task[(Marking, Output)] =
    transitionFunctions(t).asInstanceOf[(Marking, S, Input) => Task[(Marking, Output)]]

  def fireTransition[Input, Output](t: Transition[Input, Output, S]): (Marking, S, Input) => Task[(Marking, Output)] = {
    (consume, state, input) =>
      def handleFailure: PartialFunction[Throwable, Task[(Marking, Output)]] = { case e: Throwable =>
        Task.fail(new TransitionFailedException(t, e))
      }

      if (consume.multiplicities != inMarking(t)) {
        // TODO make more explicit what is wrong here, mention the first multiplicity that is incorrect.
        Task.fail(new IllegalArgumentException(s"Transition $t may not consume $consume"))
      }

      try {
        tfn(t)(consume, state, input).handleWith { handleFailure }
      } catch { handleFailure }
  }
}
