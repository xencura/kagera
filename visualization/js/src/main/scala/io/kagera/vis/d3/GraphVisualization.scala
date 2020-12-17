package io.kagera.vis.d3

import d3v4._
import d3v4.d3selection.{ BaseDom, Selection, Update }
import io.kagera.api.colored.{ Arc, ColoredPetriNet, Marking, Place, Transition }
import io.kagera.api.multiset.MultiSet
import io.kagera.vis.d3.PetriNetD3Visualization._
import org.scalajs.dom.raw.EventTarget

import scala.scalajs.js
import scala.scalajs.js.UndefOr

// Adapted from https://github.com/kyouko-taiga/petri-js
object PetriNetD3Visualization {
  def apply(element: String, petriNetModel: ColoredPetriNet, marking: Marking = Marking.empty) = {
    val uisForPlaces = petriNetModel.places.map(p => p -> PlaceUI(p)).toMap
    val uisForTransitions =
      petriNetModel.transitions.map(t => t -> TransitionUI(t)).toMap

    val placeUIs = uisForPlaces.values.toSeq
    val transitionUIs = uisForTransitions.values.toSeq
    val tokenUIs =
      marking
        .flatMap { case (place: Place[_], tokens: MultiSet[_]) =>
          tokens.flatMap { case (value, count) => (0 to count).map(_ => value -> place) }
        }
        .map { case (token, place) =>
          TokenUI(token.hashCode().toString, token, uisForPlaces(place))
        }
        .toSeq

    def uiForNode(e: Either[Place[_], Transition[_, _, _]]): Vertex = e match {
      case Left(p) => placeUIs.find(e => e.place == p).get
      case Right(t) => transitionUIs.find(e => e.transition == t).get
    }

    val edges = petriNetModel.edges.map(e => ArcUi(e, uiForNode(e.from), uiForNode(e.to)))
    new PetriNetD3Visualization(
      element,
      renderers = Seq[PositionedRenderer[_ <: Vertex]](
        new PlacePositionedRenderer(placeUIs),
        new TokenPositionedRenderer(tokenUIs),
        new TransitionPositionedRenderer(transitionUIs)
      ),
      edges = edges.toSeq
    )
  }
  val TRANSITION_SIDE = 30
  val PLACE_RADIUS = math.sqrt(TRANSITION_SIDE * TRANSITION_SIDE / 2)

  trait Vertex extends SimulationNodeImpl {
    def id: String
  }

  case class PlaceUI(place: Place[_]) extends Vertex {
    def id: String = place.id.toString
  }
  case class TransitionUI(transition: Transition[_, _, _]) extends Vertex {
    override def id: String = transition.id.toString
  }
  case class ArcUi(arc: Arc, source: Vertex, target: Vertex) extends SimulationLinkImpl[Vertex, Vertex] {
    def id: String = arc.hashCode.toString
    def label: String = s"${source.id}->${target.id}"
  }
  case class TokenUI(id: String, value: Any, var place: PlaceUI) extends Vertex

  type SelectionRenderer[T] = d3selection.Selection[T] => BaseDom[T, _ <: BaseDom[T, _]]
  type VertexRenderer = SelectionRenderer[Vertex]
  val defaultPlaceRenderer: SelectionRenderer[PlaceUI] = { places =>
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
      .text((place) => place.id)
  }
  val defaultTransitionRenderer: SelectionRenderer[TransitionUI] = { transitions =>
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
      .attr("alignment-baseline", "central")
      .text((transition) => transition.id)
  }
  val defaultTokenRenderer: SelectionRenderer[TokenUI] =
    _.append("circle")
      .attr("r", PLACE_RADIUS * 0.8)
      .attr("fill", "rgb(0, 0, 0)")
      .attr("stroke", "rgb(224, 220, 191)")
      .attr("stroke-width", "3px")
  trait PositionedRenderer[T] {
    type UiElement = T
    def uiElements: Seq[T]
    def name: String
    def draggable: Boolean
    def renderer: SelectionRenderer[T]
    def position(t: T): (UndefOr[Double], UndefOr[Double])
    def updatePositions(selection: Selection[T]): Selection[T]
  }
  trait NodeRenderer[T <: Vertex] extends PositionedRenderer[T] {
    override def updatePositions(selection: Selection[T]): Selection[T] =
      selection
        .attr(
          "transform",
          { d: T =>
            val (x, y) = position(d)
            s"translate($x, $y)"
          }
        )
  }
  class TokenPositionedRenderer(val uiElements: Seq[TokenUI]) extends NodeRenderer[TokenUI] {
    override def name: String = "token"
    override def draggable = false
    override def renderer: SelectionRenderer[TokenUI] = defaultTokenRenderer
    override def position(t: TokenUI): (UndefOr[Double], UndefOr[Double]) = (t.place.x, t.place.y)

  }
  class PlacePositionedRenderer(val uiElements: Seq[PlaceUI]) extends NodeRenderer[PlaceUI] {
    override def name: String = "place"
    override def draggable: Boolean = true
    override def renderer: SelectionRenderer[PlaceUI] = defaultPlaceRenderer
    override def position(t: PlaceUI): (UndefOr[Double], UndefOr[Double]) = (t.x, t.y)
  }
  class TransitionPositionedRenderer(val uiElements: Seq[TransitionUI]) extends NodeRenderer[TransitionUI] {
    override def name: String = "transition"
    override def draggable: Boolean = true
    override def renderer: SelectionRenderer[TransitionUI] = defaultTransitionRenderer
    override def position(t: TransitionUI): (UndefOr[Double], UndefOr[Double]) = (t.x, t.y)
  }
}

/**
 * Class representing a Petri Net simulator.
 *
 * This class holds the state of a Petri Net simulator, and renders its view. The semantics of the underlying Petri Net
 * model is customizable (see constructor), but defaults to Place/Transition nets (see
 * https://en.wikipedia.org/wiki/Petri_net).
 */
class PetriNetD3Visualization(element: String, renderers: Seq[PositionedRenderer[_ <: Vertex]], edges: Seq[ArcUi]) {

  /**
   * Creates a Petri Net simulator.
   *
   * @param {HTMLElement}
   *   element - An SVG node the simulator will be rendered to.
   * @param {Object}
   *   model - The Petri Net model to simulate.
   * @param {Object}
   *   options - Additional options
   */

  val svg = d3.select(element)
  val width = svg.node().getBoundingClientRect().width
  val height = svg.node().getBoundingClientRect().height

  // Build the arrow en marker. Note that arrows are drawn like that: ``-->-``. Hence we should draw
  // their source and target nodes over them, so as to hide the exceeding parts.
  this.svg
    .append("svg:defs")
    .selectAll("marker")
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

  val arcsGroup = this.svg.append("g").attr("class", "arcs")
  val rendererNodeGroups =
    renderers.map(renderer => RendererWithGroup(renderer, this.svg.append("g").attr("class", renderer.name + "s")))

  // Create the force simulation.
  val simulation = d3
    .forceSimulation()
    // TODO .force("link", d3.forceLink().id((d) => d.id).distance(50))
    .force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(width / 2, height / 2))
    .force("collide", d3.forceCollide[SimulationNode]().radius((_: SimulationNode) => TRANSITION_SIDE * 2.0))
    .on(
      "tick",
      () => {
        rendererNodeGroups.foreach { case rg =>
          rg.renderer.updatePositions(
            rg.group
              .selectAll[rg.renderer.UiElement]("g")
          )
        }
        this.arcsGroup
          .selectAll[ArcUi]("g line")
          .attr("x1", d => d.source.x)
          .attr("y1", d => d.source.y)
          .attr("x2", d => d.target.x)
          .attr("y2", d => d.target.y)
        this.arcsGroup
          .selectAll[ArcUi]("g text")
          .attr("x", d => (d.source.x.get + d.target.x.get) / 2)
          .attr("y", d => (d.source.y.get + d.target.y.get) / 2)
      }
    )

  def handleDragStart(d: Vertex): Unit = {
    if (d3.event.active == 0) this.simulation.alphaTarget(0.3).restart()
    d.fx = d.x
    d.fy = d.y

  }

  def handleDrag(d: Vertex): Unit = {
    d.fx = d3.event.x
    d.fy = d3.event.y

  }

  def handleDragEnd(d: Vertex): Unit = {
    if (d3.event.active == 0) this.simulation.alphaTarget(0)
    d.fx = null
    d.fy = null

  }
  case class RendererWithGroup[T <: Vertex](renderer: PositionedRenderer[T], group: Selection[EventTarget]) {
    def selector: Selection[T] = group.selectAll[T]("g")
  }

  def render() = {
    // Draw new places and new transitions.
    var arcs = this.arcsGroup
      .selectAll[ArcUi]("g")
      .data(js.Array(edges.toSeq: _*), (d: ArcUi) => d.id)
    arcs.exit().remove()

    val arcsEnter = arcs
      .enter()
      .append("g")
      .attr("id", _.id)
    arcsEnter
      .append("line")
      .attr("stroke", "black")
      .attr("stroke-width", 1)
      .attr("marker-end", "url(#end)")
    arcsEnter
      .filter(_.id != "1")
      .append("text")
      .text(_.label)

    arcs = arcsEnter.merge(arcs)

    rendererNodeGroups.foreach { case rg @ RendererWithGroup(renderer, _) =>
      val nodesy = rg.selector
        .data(js.Array(renderer.uiElements: _*), _.id)

      val nodesEnterNoDrag = nodesy
        .enter()
        .append("g")
        .attr("id", _.id)
        .attr("class", renderer.name)
      val nodesEnter =
        if (renderer.draggable)
          nodesEnterNoDrag
            .call(
              d3.drag[Vertex]()
                .on("start", handleDragStart _)
                .on("drag", handleDrag _)
                .on("end", handleDragEnd _)
            )
        else nodesEnterNoDrag

      renderer.renderer(nodesEnter)
    }
    this.simulation
      .nodes(js.Array(rendererNodeGroups.filter(_.renderer.draggable).flatMap(_.renderer.uiElements): _*))
      .force("link", d3.forceLink(js.Array(edges.toSeq: _*)))

  }

}
