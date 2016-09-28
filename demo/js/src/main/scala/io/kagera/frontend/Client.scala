package io.kagera.frontend

import scalatags.JsDom.all._
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.dom
import dom.html
import dom.ext.Ajax
import scalajs.js.annotation.JSExport

@JSExport
object Client extends {
  @JSExport
  def main(container: html.Div) = {

    val inputBox = input.render
    val outputBox = ul.render

    def refreshIndex() = Ajax.get("/process/dot").foreach { xhr =>
      outputBox.innerHTML = ""
      xhr.responseText
    }

    inputBox.onkeyup = (e: dom.Event) => refreshIndex()
    refreshIndex()

    container.appendChild(div(h1("Process index"), inputBox, outputBox).render)
  }
}
