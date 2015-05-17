package io.process.designer

import io.process.draw._
import io.process.geometry._
import io.process.designer.model.SimpleLayer

import scala.util.Random

object PetriNetModel {

  type Place = Int
  type Transition = Int
  type Arc = (Int, (Int, Int))

  val r = 10.0

  case class PetriNet(places: Set[Place], transitions: Set[Transition], edges: Set[Arc])

  //  gc.arc(-r, -r, r, 0.0, Math.PI * 2, false)
  val placeShape = Circle(r)
  val placeDrawable: Drawing = Seq(Fill("white", placeShape), Stroke("#7777ff", placeShape))
  val transitionDrawable: Drawing = Stroke("#7777ee", Rect((-r, -r), 2 * r, 2 * r))

  val placeLayer = new SimpleLayer[Place](Map.empty, placeDrawable)
  val transitionLayer = new SimpleLayer[Transition](Map.empty, transitionDrawable)

  def placeProvider(): Place = Random.nextInt()
  def transitionProvider(): Transition = Random.nextInt()
}
