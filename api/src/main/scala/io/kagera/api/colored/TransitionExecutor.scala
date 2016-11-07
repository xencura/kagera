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

class TransitionExecutorImpl[S](topology: PetriNet[Place[_], Transition[_, _, _]]) extends TransitionExecutor[S] {

  val transitionFunctions: Map[Transition[_, _, _], _] =
    topology.transitions.map(t => t -> t.apply(topology.inMarking(t), topology.outMarking(t))).toMap

  def tfn[Input, Output](t: Transition[Input, Output, S]): TransitionFunction[Input, Output, S] =
    transitionFunctions(t).asInstanceOf[TransitionFunction[Input, Output, S]]

  def fireTransition[Input, Output](t: Transition[Input, Output, S]): (Marking, S, Input) => Task[(Marking, Output)] = {
    (consume, state, input) =>
      def handleFailure: PartialFunction[Throwable, Task[(Marking, Output)]] = { case e: Throwable =>
        Task.fail(e)
      }

      if (consume.multiplicities != topology.inMarking(t)) {
        // TODO make more explicit what is wrong here, mention the first multiplicity that is incorrect.
        Task.fail(new IllegalArgumentException(s"Transition $t may not consume $consume"))
      }

      try {
        tfn(t)(consume, state, input).handleWith { handleFailure }
      } catch { handleFailure }
  }
}
