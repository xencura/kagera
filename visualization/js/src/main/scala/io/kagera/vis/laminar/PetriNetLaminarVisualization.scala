package io.kagera.vis.laminar

import cats._
import cats.effect.Sync
import cats.effect.kernel.{ CancelScope, Poll }
import com.raquo.laminar.api.L.svg._
import com.raquo.laminar.api.L.{ child, children, EventBus, Signal, Var }
import com.raquo.laminar.api._
import com.raquo.laminar.nodes.ReactiveSvgElement
import d3v4.{ d3, SimulationLinkImpl, SimulationNode, SimulationNodeImpl }
import io.kagera.api.colored
import io.kagera.api.colored.{ Arc, ExecutablePetriNet, Marking, Node, Place, Transition }
import org.scalajs.dom.raw.Element
import org.scalajs.dom.svg.G

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import scala.util.Random
object PetriNetLaminarVisualization {

  case class Attachment(source: Any, target: Any)
  def apply(
    element: Element,
    petriNetModel: Signal[ExecutablePetriNet[Any]],
    marking: Marking = Marking.empty,
    //tokenRenderer: SelectionRenderer[NodeUI[Any]] = defaultTokenRenderer,
    attachments: Seq[Attachment] = Seq()
  ): PetriNetLaminarVisualization = {
    new PetriNetLaminarVisualization(element, petriNetModel, marking)

  }

  val TRANSITION_SIDE = 30
  val PLACE_RADIUS = math.sqrt(TRANSITION_SIDE * TRANSITION_SIDE / 2)
}

class PetriNetLaminarVisualization(
  element: Element,
  petriNet: Signal[ExecutablePetriNet[Any]],
  initialMarking: Marking
) {
  val canvasWidth = element.clientWidth
  val canvasHeight = element.clientHeight
  case class Positioned[+T](elem: T, coordinates: Var[(Double, Double)]) {
    def x: Signal[Double] = coordinates.signal.map(_._1)
    def y: Signal[Double] = coordinates.signal.map(_._2)
    def xAsString: Signal[String] = x.map(_.toString)
    def yAsString: Signal[String] = y.map(_.toString)
    def translateCoordinates: Signal[String] = coordinates.signal.map { case (x, y) => s"translate($x,$y)" }
  }

  val transitionsFiredBus = new EventBus[Transition[_, _, _]]
  implicit val idApplicativeError = new ApplicativeError[Id, Throwable] with Sync[Id] {
    override def raiseError[A](e: Throwable): Id[A] = {
      println(s"In raiseError with: $e")
      throw e
    }
    override def handleErrorWith[A](fa: Id[A])(f: Throwable => Id[A]): Id[A] = fa
    override def pure[A](x: A): Id[A] = x
    override def ap[A, B](ff: Id[A => B])(fa: Id[A]): Id[B] = ff(fa)

    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)

    override def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] =
      f(a).getOrElse(throw new RuntimeException(s"Got error"))

    override def suspend[A](hint: Sync.Type)(thunk: => A): Id[A] = thunk

    override def monotonic: Id[FiniteDuration] = FiniteDuration(0, TimeUnit.NANOSECONDS)

    override def realTime: Id[FiniteDuration] = monotonic

    override def rootCancelScope: CancelScope = CancelScope.Uncancelable

    override def forceR[A, B](fa: Id[A])(fb: Id[B]): Id[B] = fb

    override def uncancelable[A](body: Poll[Id] => Id[A]): Id[A] = body(new Poll[Id] {
      override def apply[A](fa: Id[A]): Id[A] = fa
    })

    override def canceled: Id[Unit] = ()

    override def onCancel[A](fa: Id[A], fin: Id[Unit]): Id[A] = fa
  }
  val marking: Signal[Marking] =
    transitionsFiredBus.events.combineWith(petriNet.changes).foldLeft(initialMarking) { case (mrk, (trns, petriNet)) =>
      println(s"Trying to fire $trns in $mrk")
      val transitionFunction = trns
        .asInstanceOf[Transition[Any, Any, Any]]
        .apply[Id](petriNet.inMarking(trns), petriNet.outMarking(trns))
      val (res, _) = transitionFunction
        .apply(mrk, ().asInstanceOf[Any], "YO".asInstanceOf[Any])
      res
    }

  val transitionWidth = 50
  val transitionHeight = transitionWidth * 2
  val placeRadius = math.sqrt(transitionWidth * transitionWidth / 2)
  val tokenSize = transitionWidth * 0.8
  def renderPlace(
    id: Long,
    initialPlace: Positioned[Place[_]],
    positionedPlace: Signal[Positioned[Place[_]]]
  ): ReactiveSvgElement[G] = g(
    cls("place"),
    transform <-- positionedPlace.flatMap(_.translateCoordinates),
    circle(r(placeRadius.toString), fill("rgb(255, 248, 220)"), stroke("rgb(224, 220, 191)"), strokeWidth("3px")),
    text(textAnchor("left"), dy((placeRadius * 1.5).toString), child.text <-- positionedPlace.map(_.elem.label))
  )
  def renderTransition(
    id: Long,
    initialTransition: Positioned[Transition[_, _, _]],
    positionedTransition: Signal[Positioned[Transition[_, _, _]]]
  ): ReactiveSvgElement[G] = {
    val isFireable =
      marking.signal.combineWith(positionedTransition).combineWith(petriNet).map {
        case (marking: Marking, Positioned(transition, _), petriNet) =>
          petriNet.isEnabled(marking)(transition)
      }
    g(
      cls("transition"),
      transform <-- positionedTransition.flatMap(_.translateCoordinates),
      rect(
        width(transitionWidth.toString),
        height(transitionHeight.toString),
        x((-transitionWidth / 2).toString),
        y((-transitionHeight / 2).toString),
        fill <-- isFireable.map {
          case true => "rgb(0, 227, 0)"
          case false => "rgb(220, 227, 255)"
        },
        stroke("rgb(169, 186, 255)"),
        strokeWidth("3"),
        cursorAttr <-- isFireable.map {
          case true => "pointer"
          case false => "default"
        },
        L.onClick.stopPropagation.preventDefault.mapTo(initialTransition.elem) --> transitionsFiredBus
      ),
      text(textAnchor("middle"), cursor("pointer"), child.text <-- positionedTransition.map(_.elem.label))
    )
  }

  private val maxStartPosition = 100
  private val positionedPlaces =
    petriNet.map(
      _.places.map(p => Positioned(p, Var((Random.nextInt(maxStartPosition), Random.nextInt(maxStartPosition)))))
    )
  private val positionedTransitions =
    petriNet.map(
      _.transitions.map(p => Positioned(p, Var((Random.nextInt(maxStartPosition), Random.nextInt(maxStartPosition)))))
    )

  def lookupNode(n: Node): Signal[Positioned[_]] = n match {
    case Left(place) =>
      positionedPlaces.map(ps =>
        ps.find(p => p.elem == place)
          .getOrElse(throw new RuntimeException(s"Could not find place $place in $ps"))
      )
    case Right(transition) =>
      positionedTransitions.map(ts =>
        ts.find(t => t.elem == transition)
          .getOrElse(throw new RuntimeException(s"Could not find transition $transition in $ts"))
      )
  }
  def renderEdge(id: Int, edge: Arc, edgeSignal: Signal[Arc]): ReactiveSvgElement[G] = {
    val fromNode = lookupNode(edge.from)
    val toNode = lookupNode(edge.to)
    g(
      cls("edge"),
      line(
        stroke("black"),
        strokeWidth("1"),
        markerEnd("url(#end)"),
        x1 <-- fromNode.flatMap(_.xAsString),
        y1 <-- fromNode.flatMap(_.yAsString),
        x2 <-- toNode.flatMap(_.xAsString),
        y2 <-- toNode.flatMap(_.yAsString)
      )
    )
  }
  def renderToken(key: Int, positionedPlace: Signal[Positioned[_]], token: Signal[String]): ReactiveSvgElement[G] = g(
    cls("token"),
    transform <-- positionedPlace.flatMap(_.translateCoordinates),
    image(
      xlinkHref <-- token.map(_ + ".svg"),
      width(tokenSize.toString),
      height(tokenSize.toString),
      x((-tokenSize / 2).toString),
      y((-tokenSize / 2).toString)
    ),
    //circle(r((placeRadius * 0.7).toString), fill("rgb(0, 0, 0)"), stroke("rgb(0, 0, 0)"), strokeWidth("3px")),
    text(textAnchor("left"), dx(placeRadius.toString), child.text <-- token)
  )
  def tokens = g(
    cls("tokens"),
    children <-- marking.signal
      .map(m => {
        m.flatMap { case (place, tokens) =>
          tokens.keys.map(_ -> place)
        }.toSeq
      })
      .split(_.hashCode()) { case (key, (token, place), placeTokenSignal) =>
        renderToken(key, lookupNode(Left(place)).signal, placeTokenSignal.map(_._1.toString))
      }
  )
  val places =
    g(
      cls("places"),
      children <-- positionedPlaces
        .map(_.toSeq)
        .split(_.elem.id)(renderPlace)
    )
  val transitions =
    g(cls("transitions"), children <-- positionedTransitions.map(_.toSeq).split(_.elem.id)(renderTransition))
  val edges = g(cls("edges"), children <-- petriNet.map(_.edges.toSeq).split(_.hashCode())(renderEdge))

  class PetriNetSimulationNode extends SimulationNodeImpl
  class PetriNetSimulationLink(val source: PetriNetSimulationNode, val target: PetriNetSimulationNode)
      extends SimulationLinkImpl[PetriNetSimulationNode, PetriNetSimulationNode]
  def render(): Unit = {
    val layout =
      petriNet.combineWith(positionedPlaces.combineWith(positionedTransitions)).map { case (pn, places, transitions) =>
        println(s"Place coordinates: ${places.map(_.coordinates.now())}")
        val layoutNodeMap =
          (places.asInstanceOf[Iterable[Positioned[_]]] ++ transitions
            .asInstanceOf[Iterable[Positioned[_]]]).map { n =>
            n -> new PetriNetSimulationNode
          }.toMap
        def findNode(n: colored.Node): PetriNetSimulationNode = n match {
          case Left(place) => layoutNodeMap.find(p => p._1.elem == place).get._2
          case Right(transition) => layoutNodeMap.find(t => t._1.elem == transition).get._2
        }

        val edges = js.Array(pn.edges.map(a => new PetriNetSimulationLink(findNode(a.from), findNode(a.to))).toSeq: _*)
        d3
          .forceSimulation[SimulationNode](js.Array(layoutNodeMap.values.toSeq: _*))
          .force("charge", d3.forceManyBody())
          .force("center", d3.forceCenter(canvasWidth / 2, canvasHeight / 2))
          .force("collide", d3.forceCollide[SimulationNode]().radius((_: SimulationNode) => 30 * 2.0))
          .force("links", d3.forceLink(edges))
          .on(
            "tick",
            () => {
              layoutNodeMap.foreach { case (positioned, simNode) =>
                positioned.coordinates.update(_ => (simNode.x.getOrElse(0.0), simNode.y.getOrElse(0.0)))
              }
            }
          )
      }
    val graph = svg(
      defs(
        // Build the arrow en marker. Note that arrows are drawn like that: ``-->-``. Hence we should draw
        // their source and target nodes over them, so as to hide the exceeding parts.
        marker(
          idAttr("end"),
          refX(transitionWidth.toString),
          refY("4"),
          markerWidth("12"),
          markerHeight("12"),
          orient("auto"),
          path(d("M0,0 L0,8 L8,4 z"))
        )
      ),
      edges,
      places,
      transitions,
      tokens,
      L.onMountCallback(ctx => layout.foreach(_.restart())(ctx.owner))
    )
    L.render(element, graph)
  }
}
