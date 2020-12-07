package io.kagera.vis.d3

import d3v4._
import d3v4.d3selection.{ BaseDom, BaseSelection }
import io.kagera.api.colored.{ Arc, ColoredPetriNet, Place, Transition }
import io.kagera.vis.d3.PetriNetD3Visualization._

import scala.scalajs.js

// Adapted from https://github.com/kyouko-taiga/petri-js
object PetriNetD3Visualization {
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

  type VertexRenderer = d3selection.Selection[Vertex] => BaseDom[Vertex, _ <: BaseDom[Vertex, _]]
  val defaultPlaceRenderer: VertexRenderer =
    _.append("circle")
      .attr("r", PLACE_RADIUS)
      .attr("fill", "rgb(255, 248, 220)")
      .attr("stroke", "rgb(224, 220, 191)")
      .attr("stroke-width", "3px")
  val defaultTransitionRenderer: VertexRenderer =
    _.append("rect")
      .attr("width", TRANSITION_SIDE)
      .attr("height", TRANSITION_SIDE)
      .attr("x", -TRANSITION_SIDE / 2)
      .attr("y", -TRANSITION_SIDE / 2)
      .attr("fill", "rgb(220, 227, 255)")
      .attr("stroke", "rgb(169, 186, 255)")
      .attr("stroke-width", 3)
}

/**
 * Class representing a Petri Net simulator.
 *
 * This class holds the state of a Petri Net simulator, and renders its view. The semantics of the underlying Petri Net
 * model is customizable (see constructor), but defaults to Place/Transition nets (see
 * https://en.wikipedia.org/wiki/Petri_net).
 */
class PetriNetD3Visualization(
  element: String,
  petriNetModel: ColoredPetriNet,
  placeRenderer: VertexRenderer = defaultPlaceRenderer,
  transitionRenderer: VertexRenderer = PetriNetD3Visualization.defaultTransitionRenderer
) {
  val uisForPlaces: Map[Place[_], PlaceUI] = petriNetModel.places.map(p => p -> PlaceUI(p)).toMap
  val uisForTransitions: Map[Transition[_, _, _], TransitionUI] =
    petriNetModel.transitions.map(t => t -> TransitionUI(t)).toMap
  val nodesUIMap: Map[Object, Vertex] = (uisForPlaces ++ uisForTransitions).toMap

  def uiForNode(e: Either[Place[_], Transition[_, _, _]]): Vertex = e match {
    case Left(p) => uisForPlaces(p)
    case Right(t) => uisForTransitions(t)
  }

  val edges = petriNetModel.edges.map(e => ArcUi(e, uiForNode(e.from), uiForNode(e.to)))
  val placeUIs = uisForPlaces.values.toSeq
  val transitionUIs = uisForTransitions.values.toSeq

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
  val nodesGroup = this.svg.append("g").attr("class", "nodes")

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
        this.nodesGroup
          .selectAll[Vertex]("g")
          .attr("transform", (d) => s"translate(${d.x}, ${d.y})")
        this.arcsGroup
          .selectAll[ArcUi]("g line")
          .attr("x1", (d) => d.source.x)
          .attr("y1", (d) => d.source.y)
          .attr("x2", (d) => d.target.x)
          .attr("y2", (d) => d.target.y)
        this.arcsGroup
          .selectAll[ArcUi]("g text")
          .attr("x", (d) => (d.source.x.get + d.target.x.get) / 2)
          .attr("y", (d) => (d.source.y.get + d.target.y.get) / 2)
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

  def render() = {
    // Adapt places and transitions data to d3. The goal is to create an array that contains all
    // vertices and another that contains all edges, so that it'll be easier to handle them in the
    // force simulation later on.
    val vertices = js.Array[Vertex](placeUIs.concat(transitionUIs): _*)
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

    val nodes = this.nodesGroup
      .selectAll[Vertex]("g")
      .data(vertices, _.id)

    val nodesEnter = nodes
      .enter()
      .append("g")
      .attr("id", _.id)
      .attr(
        "class",
        _ match {
          case _: PlaceUI => "place"
          case _: TransitionUI => "transition"
        }
      )
      .call(
        d3.drag[Vertex]()
          .on("start", handleDragStart _)
          .on("drag", handleDrag _)
          .on("end", handleDragEnd _)
      )

    val places = nodesEnter.filter(".place")
    placeRenderer(places)
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

    val transitions = nodesEnter
      .filter(".transition")
      .attr("cursor", "pointer")
    transitions
      .append("circle")
      .attr("r", PLACE_RADIUS)
      .attr("fill", "white")
    transitionRenderer(transitions)
    transitions
      .append("text")
      .attr("text-anchor", "middle")
      .attr("alignment-baseline", "central")
      .text((transition) => transition.id)
    // Run the force simulation to space out places and transitions.
    this.simulation
      .nodes(vertices.map(_.asInstanceOf[SimulationNode]))
      .force("link", d3.forceLink(js.Array(edges.toSeq: _*)))

  }

}
