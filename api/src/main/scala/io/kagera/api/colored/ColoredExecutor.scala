package io.kagera.api.colored

import io.kagera.api._

import scala.concurrent.{ ExecutionContext, Future }

trait ColoredExecutor extends TransitionExecutor[Place, Transition, ColoredMarking] {

  this: PetriNet[Place, Transition] with TokenGame[Place, Transition, ColoredMarking] =>

  def executeTransition(
    pn: PetriNet[Place, Transition]
  )(consume: ColoredMarking, t: Transition, extraData: Option[Any], id: java.util.UUID)(implicit
    ec: ExecutionContext
  ): Future[ColoredMarking] = {

    try {

      val inAdjacent = consume.map { case (place, data) =>
        (place, pn.innerGraph.findPTEdge(place, t).get.label.asInstanceOf[PTEdge[Any]], data)
      }.toSeq

      val outAdjacent = pn
        .outAdjacentPlaces(t)
        .map { case place =>
          (pn.innerGraph.findTPEdge(t, place).get, place)
        }
        .toSeq

      val input = t.createInput(inAdjacent, extraData, TransitionContext(id))

      t.apply(input).map(t.createOutput(_, outAdjacent))

    } catch {
      case e: Exception => Future.failed(e)
    }
  }

  override def fireTransition(marking: ColoredMarking, id: java.util.UUID)(t: Transition, data: Option[Any])(implicit
    ec: ExecutionContext
  ) = {

    // pick the tokens
    enabledParameters(marking)
      .get(t)
      .flatMap(_.headOption)
      .map { consume =>
        executeTransition(this)(consume, t, data, id)
          .recoverWith { case e: Exception =>
            Future.failed(new RuntimeException(s"Transition '$t' failed to fire!", e))
          }
          .map(produce => marking.consume(consume).produce(produce))

      }
      .getOrElse { throw new IllegalStateException(s"Transition $t is not enabled") }
  }
}
