package io.kagera.api.colored

import io.kagera.api._

import scala.concurrent.{ ExecutionContext, Future }

trait TransitionExecutor[S] {

  this: PetriNet[Place[_], Transition] with TokenGame[Place[_], Transition, ColoredMarking] =>

  implicit val executionContext: ExecutionContext

  val transitionFunctions: Map[Transition, _] =
    transitions.map(t => t -> t.apply(inMarking(t), outMarking(t))).toMap

  def tfn(t: Transition): (ColoredMarking, t.Context, t.Input) => Future[(ColoredMarking, t.Context, t.Output)] =
    transitionFunctions(t)
      .asInstanceOf[(ColoredMarking, t.Context, t.Input) => Future[(ColoredMarking, t.Context, t.Output)]]

  def fireTransition(t: Transition { type Input = Unit })(marking: ColoredMarking, context: S) = ???

  def fireTransition(t: Transition { type Context = S })(marking: ColoredMarking, context: S, input: t.Input) = {

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
