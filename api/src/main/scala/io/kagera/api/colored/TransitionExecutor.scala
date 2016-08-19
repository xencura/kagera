package io.kagera.api.colored

import io.kagera.api._

import scala.concurrent.{ ExecutionContext, Future }

trait TransitionExecutor[S] {

  this: PetriNet[Place[_], Transition[_, _, _]] with TokenGame[Place[_], Transition[_, _, _], ColoredMarking] =>

  // TODO remove this requirement
  implicit def executionContext: ExecutionContext

  val transitionFunctions: Map[Transition[_, _, _], _] =
    transitions.map(t => t -> t.apply(inMarking(t), outMarking(t))).toMap

  def tfn[Input, Output](
    t: Transition[Input, Output, S]
  ): (ColoredMarking, S, Input) => Future[(ColoredMarking, Output)] =
    transitionFunctions(t).asInstanceOf[(ColoredMarking, S, Input) => Future[(ColoredMarking, Output)]]

  def fireTransition[Input, Output](
    t: Transition[Input, Output, S]
  )(consume: ColoredMarking, state: S, input: Input): Future[(ColoredMarking, Output)] = {

    if (consume.multiplicities != inMarking(t)) {
      // TODO make more explicit what is wrong here, mention the first multiplicity that is incorrect.
      Future.failed(new IllegalArgumentException(s"Transition $t may not consume $consume"))
    }

    def handleFailure: PartialFunction[Throwable, Future[(ColoredMarking, Output)]] = { case e: Throwable =>
      Future.failed(new TransitionFailedException(t, e))
    }

    try {
      tfn(t)(consume, state, input).recoverWith { handleFailure }
    } catch { handleFailure }
  }
}
