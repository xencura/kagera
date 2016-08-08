package io.kagera.api.colored

import io.kagera.api._

import scala.concurrent.{ ExecutionContext, Future }

trait TransitionExecutor[S] {

  this: PetriNet[Place[_], Transition[_, _, _]] with TokenGame[Place[_], Transition[_, _, _], ColoredMarking] =>

  implicit val executionContext: ExecutionContext

  val transitionFunctions: Map[Transition[_, _, _], _] =
    transitions.map(t => t -> t.apply(inMarking(t), outMarking(t))).toMap

  def tfn[Input, Output](
    t: Transition[Input, Output, S]
  ): (ColoredMarking, S, Input) => Future[(ColoredMarking, S, Output)] =
    transitionFunctions(t).asInstanceOf[(ColoredMarking, S, Input) => Future[(ColoredMarking, S, Output)]]

  def fireTransition[Input, Output](
    t: Transition[Input, Output, S]
  )(marking: ColoredMarking, context: S, input: Input): Future[(ColoredMarking, S)] = {

    // pick the tokens
    val result = enabledParameters(marking)
      .get(t)
      .flatMap(_.headOption)
      .map { consume =>
        tfn(t)(consume, context, input)
          .recoverWith { case e: Exception =>
            Future.failed(new RuntimeException(s"Transition '$t' failed to fire!", e))
          }
          .map { case (produce, context, tOut) => (marking -- consume ++ produce, context) }

      }
      .getOrElse { throw new IllegalStateException(s"Transition $t is not enabled") }

    result
  }
}
