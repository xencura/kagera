package io.kagera.frontend.cytoscape

import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

@JSName("cytoscape")
@js.native
object CytoScape extends js.Object {
  def apply(data: js.Object): Foo = js.native

}

@js.native
trait Foo extends js.Object {
  def forceRender() = js.native
}
