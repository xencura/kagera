package io.kagera.execution

import cats.ApplicativeError
import cats.effect.Sync
import io.kagera.api.colored.{ColoredPetriNet, Marking, Transition, TransitionFunction}

trait TransitionExecutor[F[_], State] {

  /**
   * Given a transition returns an input output function
   *
   * @param t
   * @tparam Input
   * @tparam Output
   * @return
   */
  def fireTransition[Input, Output](t: Transition[Input, Output, State]): TransitionFunction[F, Input, Output, State]
}

class TransitionExecutorImpl[F[_], State](topology: ColoredPetriNet)(implicit
  sync: Sync[F],
  errorHandling: ApplicativeError[F, Throwable]
) extends TransitionExecutor[F, State] {

  val cachedTransitionFunctions: Map[Transition[_, _, _], _] =
    topology.transitions.map(t => t -> t.apply[F](topology.inMarking(t), topology.outMarking(t))).toMap

  def transitionFunction[Input, Output](t: Transition[Input, Output, State]) =
    cachedTransitionFunctions(t).asInstanceOf[TransitionFunction[F, Input, Output, State]]

  def fireTransition[Input, Output](
    t: Transition[Input, Output, State]
  ): TransitionFunction[F, Input, Output, State] = { (consume, state, input) =>
    def handleFailure: PartialFunction[Throwable, F[(Marking, Output)]] = { case e: Throwable =>
      errorHandling.raiseError(e).asInstanceOf[F[(Marking, Output)]]
    }

    if (consume.multiplicities != topology.inMarking(t)) {
      errorHandling.raiseError(new IllegalArgumentException(s"Transition $t may not consume $consume"))
    }

    try {
      errorHandling.handleErrorWith(transitionFunction[Input, Output](t)(consume, state, input)) { handleFailure }
    } catch { handleFailure }
  }
}
