package io.process.designer.model

import io.process.designer.model.Graph._
import io.process.common.draw._
import io.process.common.geometry._

import scala.util.Random

object PetriNetModel extends GraphVisuals {

  // type definition place holders
  type Id = Long
  case class Place(id: Id)
  case class Transition(id: Id)
  case class Arc(id: Id, from: Id, to: Id)
  case class PetriNet(places: Set[Place], transitions: Set[Transition], edges: Set[Arc])

  val r = 10.0
  //  gc.arc(-r, -r, r, 0.0, Math.PI * 2, false)
  val placeShape = Circle(r)
  val placeDrawable: Drawing = Seq(Fill("white", placeShape), Stroke("#7777ff", placeShape))

  val transitionShape = Rect((-r, -r), 2 * r, 2 * r)
  val transitionDrawable: Drawing = Seq(Fill("white", transitionShape), Stroke("#7777ee", transitionShape))

  def transitions(n: Int, bounds: Rectangle) = PointCloud
    .pointCloud(n, bounds)
    .map(p => VisualNode[Transition](Transition(Random.nextLong()), transitionDrawable, p))

  def places(n: Int, bounds: Rectangle) =
    PointCloud.pointCloud(n, bounds).map(p => VisualNode[Place](Place(Random.nextLong()), placeDrawable, p))

  def placeProvider(): Place = Place(Random.nextInt())
  def transitionProvider(): Transition = Transition(Random.nextInt())
}
