package io.kagera.api.colored

import fs2.Task
import io.kagera.api._

trait TransitionExecutor[State] {

  /**
   * Given a transition returns an input output function
   *
   * @param t
   * @tparam Input
   * @tparam Output
   * @return
   */
  def fireTransition[Input, Output](t: Transition[Input, Output, State]): TransitionFunction[Input, Output, State]
}

class TransitionExecutorImpl[S](topology: ColoredPetriNet) extends TransitionExecutor[S] {

  val cachedTransitionFunctions: Map[Transition[_, _, _], _] =
    topology.transitions.map(t => t -> t.apply(topology.inMarking(t), topology.outMarking(t))).toMap

  def transitionFunction[Input, Output](t: Transition[Input, Output, S]) =
    cachedTransitionFunctions(t).asInstanceOf[TransitionFunction[Input, Output, S]]

  def fireTransition[Input, Output](t: Transition[Input, Output, S]): (Marking, S, Input) => Task[(Marking, Output)] = {
    (consume, state, input) =>
      def handleFailure: PartialFunction[Throwable, Task[(Marking, Output)]] = { case e: Throwable =>
        Task.fail(e)
      }

      if (consume.multiplicities != topology.inMarking(t)) {
        Task.fail(new IllegalArgumentException(s"Transition $t may not consume $consume"))
      }

      try {
        transitionFunction(t)(consume, state, input).handleWith { handleFailure }
      } catch { handleFailure }
  }
}
