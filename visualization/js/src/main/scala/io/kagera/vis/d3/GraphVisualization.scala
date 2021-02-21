package io.kagera.vis.d3

import d3v4._
import d3v4.d3selection.{ BaseDom, Selection }
import org.scalajs.dom.raw.EventTarget

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import GraphVisualization._
import org.scalajs.dom

object GraphVisualization {
  trait HasId {
    def id: String
  }

  trait Vertex extends SimulationNodeImpl with HasId

  case class NodeUI[+T](content: T) extends Vertex {
    override def id: String = content.hashCode.toString //TODO
  }

  case class EdgeUI[+C, S, T](content: C, source: NodeUI[S], target: NodeUI[T])
      extends SimulationLinkImpl[NodeUI[S], NodeUI[T]]

  type SelectionRenderer[T] = d3selection.Selection[T] => BaseDom[T, _ <: BaseDom[T, _]]
  type VertexRenderer = SelectionRenderer[Vertex]

  case class Attachment(source: Any, target: Any)

  trait Positioning

  case object Free extends Positioning

  case class AttachedTo[T](t: NodeUI[T]) extends Positioning

  case class Transitioning[S, T](edgeUI: EdgeUI[_, S, T]) extends Positioning

  trait PositionedRenderer[C, T] {
    type UiElement = T

    def elements: Seq[C]

    def name: String

    def id(t: T): String

    def movable: Boolean

    def renderer: SelectionRenderer[T]

    def updatePositions(selection: Selection[T]): Unit

    def svgDefs: Map[String, Selection[String] => Selection[String]] = Map.empty
  }

  class NodeRenderer[E](
    val name: String,
    val elements: Seq[E],
    val renderer: SelectionRenderer[NodeUI[E]],
    val movable: Boolean = true,
    val idFunc: E => String
  ) extends PositionedRenderer[E, NodeUI[E]] {
    type Content = E

    def position(t: NodeUI[E]): (UndefOr[Double], UndefOr[Double]) = (t.x, t.y)

    override def updatePositions(selection: Selection[NodeUI[E]]): Unit =
      selection
        .attr(
          "transform",
          { d: NodeUI[E] =>
            val (x, y) = position(d)
            s"translate($x, $y)"
          }
        )

    override def id(t: NodeUI[E]): String = idFunc(t.content)
  }

  class EdgeRenderer[C, S, T](
    val elements: Seq[(C, S, T)],
    val name: String,
    val idFunc: EdgeUI[C, S, T] => String = (t: EdgeUI[C, S, T]) => s"${t.source.id}->${t.target.id}",
    val renderer: SelectionRenderer[EdgeUI[C, S, T]] = (arcsEnter: Selection[EdgeUI[Any, S, T]]) => {
      arcsEnter
        .append("line")
        .attr("stroke", "black")
        .attr("stroke-width", 1)
        .attr("marker-end", "url(#end)")
    }
  ) extends PositionedRenderer[(C, S, T), EdgeUI[C, S, T]] {
    type Content = C
    type Source = S
    type Target = T

    override def movable = false

    def createUiElements(
      sourceResolver: Source => NodeUI[Source],
      targetResolver: Target => NodeUI[Target]
    ): Seq[EdgeUI[C, S, T]] =
      elements.map { case (c, s, t) => EdgeUI(c, sourceResolver(s), targetResolver(t)) }

    override def id(t: EdgeUI[C, S, T]): String = idFunc(t)

    override def updatePositions(selection: Selection[EdgeUI[C, S, T]]): Unit = {
      selection
        .selectAll[EdgeUI[C, S, T]]("line")
        .attr("x1", d => d.source.x)
        .attr("y1", d => d.source.y)
        .attr("x2", d => d.target.x)
        .attr("y2", d => d.target.y)
      selection
        .selectAll[EdgeUI[C, S, T]]("text")
        .attr("x", d => (d.source.x.get + d.target.x.get) / 2)
        .attr("y", d => (d.source.y.get + d.target.y.get) / 2)
    }
  }

}

/**
 * Class representing a Petri Net simulator.
 *
 * This class holds the state of a Petri Net simulator, and renders its view. The semantics of the underlying Petri Net
 * model is customizable (see constructor), but defaults to Place/Transition nets (see
 * https://en.wikipedia.org/wiki/Petri_net).
 */
class GraphVisualization(
  element: String,
  nodeRenderers: Seq[NodeRenderer[_]],
  edgeRenderers: Seq[EdgeRenderer[_, _, _]],
  attachments: Seq[Attachment] = Seq()
) {

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

  val preDefs = nodeRenderers.flatMap(_.svgDefs) ++ edgeRenderers.flatMap(_.svgDefs)
  preDefs.foldLeft(
    this.svg
      .append("svg:defs")
      .asInstanceOf[Selection[String]]
  ) { case (svg, (name, mod)) => mod(svg.selectAll(name)) }

  case class NodeRendererWithUiElements[C](renderer: NodeRenderer[C], uiElements: Seq[NodeUI[C]]) {
    def attachTo(sel: Selection[dom.EventTarget]): RendererWithGroup[C, NodeUI[C]] =
      RendererWithGroup(renderer, uiElements, sel.attr("class", renderer.name + "s"))
  }

  val nodeRenderersWithUiElements =
    nodeRenderers.map(r =>
      NodeRendererWithUiElements[r.Content](
        r.asInstanceOf[NodeRenderer[r.Content]],
        r.elements.asInstanceOf[Seq[r.Content]].map(e => NodeUI[r.Content](e.asInstanceOf[r.Content]))
      )
    )
  val nodesByContent = nodeRenderersWithUiElements.flatMap(_.uiElements).map(n => n.content -> n).toMap

  case class NodeAttachment(source: NodeUI[_], target: NodeUI[_])

  val nodeAttachments = attachments.map { case Attachment(source, target) =>
    NodeAttachment(nodesByContent(source), nodesByContent(target))
  }

  case class EdgeRendererWithUiElements[C, S, T](renderer: EdgeRenderer[C, S, T], uiElements: Seq[EdgeUI[C, S, T]]) {
    def attachTo(sel: Selection[dom.EventTarget]): RendererWithGroup[(C, S, T), EdgeUI[C, S, T]] =
      RendererWithGroup(renderer, uiElements, sel.attr("class", renderer.name + "s"))
  }

  val edgeRenderersWithUiElements =
    edgeRenderers.map(r =>
      EdgeRendererWithUiElements[r.Content, r.Source, r.Target](
        r.asInstanceOf[EdgeRenderer[r.Content, r.Source, r.Target]],
        r
          .createUiElements(
            nodesByContent.asInstanceOf[r.Source => NodeUI[r.Source]],
            nodesByContent.asInstanceOf[r.Target => NodeUI[r.Target]]
          )
          .asInstanceOf[Seq[EdgeUI[r.Content, r.Source, r.Target]]]
      )
    )
  val rendererEdgeGroups: Seq[RendererWithGroup[_, _ <: EdgeUI[_, _, _]]] =
    edgeRenderersWithUiElements.map(_.attachTo(this.svg.append("g")))
  val rendererNodeGroups: Seq[RendererWithGroup[_, _ <: NodeUI[_]]] =
    nodeRenderersWithUiElements.map(_.attachTo(this.svg.append("g")))
  // Create the force simulation.
  val simulation = d3
    .forceSimulation[NodeUI[_]]()
    .force("charge", d3.forceManyBody())
    .force("center", d3.forceCenter(width / 2, height / 2))
    .force("collide", d3.forceCollide[NodeUI[_]]().radius((_: SimulationNode) => 30 * 2.0))
    .on(
      "tick",
      () => {
        nodeAttachments.foreach { case NodeAttachment(source, target) =>
          source.x = target.x
          source.y = target.y
        }
        (rendererEdgeGroups ++ rendererNodeGroups).foreach { case rg =>
          rg.renderer.updatePositions(
            rg.group
              .selectAll[rg.renderer.UiElement]("g")
          )
        }

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

  case class RendererWithGroup[C, T](
    renderer: PositionedRenderer[C, T],
    uiElements: Seq[T],
    group: Selection[EventTarget]
  ) {
    def selector: Selection[T] = group.selectAll[T]("g")
  }

  def render() = {
    // Draw new places and new transitions.
    rendererEdgeGroups.foreach { case rg @ RendererWithGroup(renderer, uiElements, _) =>
      val arcs = rg.selector
        .data(js.Array(uiElements: _*), renderer.id _)

      val arcsEnter = arcs
        .enter()
        .append("g")
        //TODO .attr("id", _.id)
        .attr("class", renderer.name)
      arcs.exit().remove()
      renderer.renderer(arcsEnter)
    }

    rendererNodeGroups.foreach { case rg @ RendererWithGroup(renderer, uiElements, _) =>
      val nodesy = rg.selector
        .data(js.Array(uiElements: _*), _.id)

      val nodesEnterNoDrag = nodesy
        .enter()
        .append("g")
        .attr("id", _.id)
        .attr("class", renderer.name)
      val nodesEnter =
        if (renderer.movable)
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
    val movableUiElements = rendererNodeGroups.filter(_.renderer.movable).flatMap(_.uiElements)
    val edgeUiElements = rendererEdgeGroups.flatMap(_.uiElements)
    this.simulation
      .nodes(js.Array[NodeUI[_]](movableUiElements: _*))
      .force("link", d3.forceLink[NodeUI[_], EdgeUI[_, _, _]](js.Array(edgeUiElements: _*)).distance(50))

  }

}
