package io.kagera.api.colored

import io.kagera.api._
import io.kagera.api.ScalaGraph._

import scala.concurrent.{ ExecutionContext, Future }

trait ColoredExecutor extends TransitionExecutor[Place, Transition, ColoredMarking] {

  this: PetriNet[Place, Transition] with TokenGame[Place, Transition, ColoredMarking] =>

  def executeTransition(
    pn: PetriNet[Place, Transition]
  )(consume: ColoredMarking, t: Transition, extraData: Option[Any])(implicit
    ec: ExecutionContext
  ): Future[ColoredMarking] = {

    try {

      val inAdjacent = consume.map { case (place, data) =>
        (place, pn.innerGraph.connectingEdgeAB(place, t), data)
      }.toSeq

      val outAdjacent = pn
        .outAdjacentPlaces(t)
        .map { case place =>
          (pn.innerGraph.connectingEdgeBA(t, place), place)
        }
        .toSeq

      val input = t.createInput(inAdjacent, extraData)

      t.apply(input).map(t.createOutput(_, outAdjacent))

    } catch {
      case e: Exception => Future.failed(e)
    }
  }

  override def fireTransition(
    marking: ColoredMarking
  )(t: Transition, data: Option[Any])(implicit ec: ExecutionContext) = {

    // pick the tokens
    enabledParameters(marking)
      .get(t)
      .flatMap(_.headOption)
      .map { consume =>
        executeTransition(this)(consume, t, data)
          .recoverWith { case e: Exception =>
            Future.failed(new RuntimeException(s"Transition '$t' failed to fire!", e))
          }
          .map(produce => marking.consume(consume).produce(produce))

      }
      .getOrElse { throw new IllegalStateException(s"Transition $t is not enabled") }
  }
}
