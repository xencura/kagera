package io.kagera.api.colored

import io.kagera.api._

import scala.concurrent.{ ExecutionContext, Future }

trait TransitionExecutor[S] {

  this: PetriNet[Place[_], Transition[_, _, _]] with TokenGame[Place[_], Transition[_, _, _], ColoredMarking] =>

  // TODO remove this requirement
  implicit val executionContext: ExecutionContext

  val transitionFunctions: Map[Transition[_, _, _], _] =
    transitions.map(t => t -> t.apply(inMarking(t), outMarking(t))).toMap

  def tfn[Input, Output](
    t: Transition[Input, Output, S]
  ): (ColoredMarking, S, Input) => Future[(ColoredMarking, Output)] =
    transitionFunctions(t).asInstanceOf[(ColoredMarking, S, Input) => Future[(ColoredMarking, Output)]]

  def applyTransition[Input, Output](
    t: Transition[Input, Output, S]
  )(marking: ColoredMarking, state: S, input: Input): Future[(ColoredMarking, S)] = {
    ???
  }

  def fireTransition[Input, Output](
    t: Transition[Input, Output, S]
  )(consume: ColoredMarking, state: S, input: Input): Future[(ColoredMarking, Output)] = {

    if (consume.multiplicities == inMarking(t)) {
      // TODO make more explicit what is wrong here, mention the first multiplicity that is incorrect.
      Future.failed(new IllegalArgumentException(s"Transition $t may not consume $consume"))
    }

    tfn(t)(consume, state, input)
      .recoverWith { case e: Exception =>
        Future.failed(new RuntimeException(s"Transition '$t' failed to fire!", e))
      }
      .map { case (produce, output) => (produce, output) }

    //    }.getOrElse { throw new IllegalStateException(s"Transition $t is not enabled") }
  }
}
