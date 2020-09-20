package io.kagera.api.colored

import cats.effect.IO
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

class TransitionExecutorImpl[State](topology: ColoredPetriNet) extends TransitionExecutor[State] {

  val cachedTransitionFunctions: Map[Transition[_, _, _], _] =
    topology.transitions.map(t => t -> t.apply(topology.inMarking(t), topology.outMarking(t))).toMap

  def transitionFunction[Input, Output](t: Transition[Input, Output, State]) =
    cachedTransitionFunctions(t).asInstanceOf[TransitionFunction[Input, Output, State]]

  def fireTransition[Input, Output](t: Transition[Input, Output, State]): TransitionFunction[Input, Output, State] = {
    (consume, state, input) =>
      def handleFailure: PartialFunction[Throwable, IO[(Marking, Output)]] = { case e: Throwable =>
        IO.raiseError(e).asInstanceOf[IO[(Marking, Output)]]
      }

      if (consume.multiplicities != topology.inMarking(t)) {
        IO.raiseError(new IllegalArgumentException(s"Transition $t may not consume $consume"))
      }

      try {
        transitionFunction(t)(consume, state, input).handleErrorWith { handleFailure }
      } catch { handleFailure }
  }
}
