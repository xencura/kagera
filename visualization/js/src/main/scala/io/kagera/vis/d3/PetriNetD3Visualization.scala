package io.kagera.vis.d3

import d3v4.d3selection.Selection

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.api.multiset._
import GraphVisualization._

object PetriNetD3Visualization {
  def apply(
    element: String,
    petriNetModel: PetriNet[Place[_], Transition[_, _, _]],
    marking: Marking = Marking.empty,
    additionalNodeRenderers: Seq[NodeRenderer[_]] = Seq(),
    additionalEdgeRenderers: Seq[EdgeRenderer[_, _, _]] = Seq(),
    tokenRenderer: SelectionRenderer[NodeUI[Any]] = defaultTokenRenderer,
    attachments: Seq[Attachment] = Seq()
  ) = {
    val (tokens, attachments) =
      marking
        .flatMap { case (place: Place[_], tokens: MultiSet[_]) =>
          tokens.flatMap { case (value, count) => (0 to count).map(_ => value -> place) }
        }
        .map { case (token, place) => token -> Attachment(token, place) }
        .toSeq
        .unzip

    val edges = petriNetModel.edges.map(e => (e, e.from.fold(identity, identity), e.to.fold(identity, identity)))
    new GraphVisualization(
      element,
      nodeRenderers = Seq[NodeRenderer[_]](
        PlaceRenderer[petriNetModel.PlaceType](petriNetModel.places.toSeq),
        TokenPositionedRenderer(tokens, tokenRenderer),
        TransitionPositionedRenderer(petriNetModel.transitions.toSeq)
      ) ++ additionalNodeRenderers,
      edgeRenderers = Seq(new ArcRenderer(edges.toSeq): EdgeRenderer[Arc, _, _]) ++ additionalEdgeRenderers,
      attachments = attachments
    )
  }

  val TRANSITION_SIDE = 30
  val PLACE_RADIUS = math.sqrt(TRANSITION_SIDE * TRANSITION_SIDE / 2)

  def defaultPlaceRenderer[T <: Place[_]]: SelectionRenderer[NodeUI[T]] = { places =>
    places
      .append("circle")
      .attr("r", PLACE_RADIUS)
      .attr("fill", "rgb(255, 248, 220)")
      .attr("stroke", "rgb(224, 220, 191)")
      .attr("stroke-width", "3px")
    places
      .append("text")
      .attr("class", "marking")
      .attr("text-anchor", "middle")
      .attr("alignment-baseline", "central")
    places
      .append("text")
      .attr("text-anchor", "left")
      .attr("alignment-baseline", "central")
      .attr("dx", PLACE_RADIUS * 1.25)
      .text((place) => place.content.label)
  }
  def defaultTransitionRenderer[T <: Transition[_, _, _]]: SelectionRenderer[NodeUI[T]] = { transitions =>
    transitions
      .append("rect")
      .attr("width", TRANSITION_SIDE)
      .attr("height", TRANSITION_SIDE)
      .attr("x", -TRANSITION_SIDE / 2)
      .attr("y", -TRANSITION_SIDE / 2)
      .attr("fill", "rgb(220, 227, 255)")
      .attr("stroke", "rgb(169, 186, 255)")
      .attr("stroke-width", 3)
      .attr("cursor", "pointer")
    transitions
      .append("text")
      .attr("text-anchor", "middle")
      .attr("cursor", "pointer")
      .text((transition) => transition.content.label)
  }

  val defaultTokenRenderer: SelectionRenderer[NodeUI[Any]] =
    _.append("circle")
      .attr("r", PLACE_RADIUS * 0.8)
      .attr("fill", "rgb(0, 0, 0)")
      .attr("stroke", "rgb(224, 220, 191)")
      .attr("stroke-width", "3px")

  object TokenPositionedRenderer {
    def apply(elements: Seq[Any], renderer: SelectionRenderer[NodeUI[Any]] = defaultTokenRenderer) =
      new NodeRenderer[Any](
        name = "token",
        elements,
        movable = false,
        renderer = renderer,
        idFunc = _.hashCode.toString
      )
  }

  object PlaceRenderer {
    def apply[T <: Place[_]](elements: Seq[T]) =
      new NodeRenderer[T](name = "place", elements, renderer = defaultPlaceRenderer[T], idFunc = _.id.toString)
  }

  object TransitionPositionedRenderer {
    def apply[T <: Transition[_, _, _]](elements: Seq[T]) =
      new NodeRenderer[T](name = "transition", elements, renderer = defaultTransitionRenderer, idFunc = _.id.toString)
  }

  class ArcRenderer[S, T](elements: Seq[(Arc, S, T)])
      extends EdgeRenderer[Arc, S, T](
        name = "arc",
        elements = elements,
        renderer = (arcsEnter: Selection[EdgeUI[Arc, S, T]]) => {
          arcsEnter
            .append("line")
            .attr("stroke", "black")
            .attr("stroke-width", 1)
            .attr("marker-end", "url(#end)")
        }
      ) {
    override def svgDefs = Map("marker" -> { (sel: Selection[String]) =>
      // Build the arrow en marker. Note that arrows are drawn like that: ``-->-``. Hence we should draw
      // their source and target nodes over them, so as to hide the exceeding parts.
      sel
        .data(js.Array("end"))
        .enter()
        .append("svg:marker")
        .attr("id", "end")
        .attr("refX", TRANSITION_SIDE)
        .attr("refY", 4)
        .attr("markerWidth", 12)
        .attr("markerHeight", 12)
        .attr("orient", "auto")
        .append("svg:path")
        .attr("d", "M0,0 L0,8 L8,4 z")
    })
  }

}
