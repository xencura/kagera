package io.kagera.api.colored

import cats.ApplicativeError
import cats.syntax.applicativeError._
import io.kagera.api._

trait TransitionExecutor[F[_], T] {

  /**
   * Given a transition returns an input output function
   *
   * @param t
   * @return
   */
  def fireTransition(t: T)(implicit
    executorFactory: TransitionExecutorFactory[F, T]
  ): TransitionFunctionF[F, executorFactory.Input, executorFactory.Output, executorFactory.State]
}

class TransitionExecutorImpl[F[_], T](topology: ColoredPetriNet[T])(implicit
  errorHandling: ApplicativeError[F, Throwable]
) extends TransitionExecutor[F, T] {

  /*
  TODO: Reintroduce caching
  val cachedTransitionFunctions: Map[T, TransitionFunctionF[F, Input, Output, State]] =
    topology.transitions.map(t => t -> executorFactory.createTransitionExecutor(t, topology.inMarking(t), topology.outMarking(t))).toMap

  def transitionFunction(t: T): TransitionFunctionF[F, Input, Output, State] = cachedTransitionFunctions(t)
   */

  def fireTransition(t: T)(implicit
    executorFactory: TransitionExecutorFactory[F, T]
  ): TransitionFunctionF[F, executorFactory.Input, executorFactory.Output, executorFactory.State] = {
    (consume, state, input) =>
      def handleFailure: PartialFunction[Throwable, F[(Marking, executorFactory.Output)]] = { case e: Throwable =>
        errorHandling.raiseError(e).asInstanceOf[F[(Marking, executorFactory.Output)]]
      }

      if (consume.multiplicities != topology.inMarking(t)) {
        errorHandling.raiseError(new IllegalArgumentException(s"Transition $t may not consume $consume"))
      }

      val transitionFunction
        : TransitionFunctionF[F, executorFactory.Input, executorFactory.Output, executorFactory.State] =
        executorFactory.createTransitionExecutor(t, topology.inMarking(t), topology.outMarking(t))
      try {
        errorHandling.handleErrorWith(transitionFunction(consume, state, input)) {
          handleFailure
        }
      } catch {
        handleFailure
      }
  }
}
