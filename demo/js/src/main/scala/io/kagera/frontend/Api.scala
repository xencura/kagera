package io.kagera.frontend

import io.kagera.demo.model.{ PetriNetModel, ProcessState }
import org.scalajs.dom.ext.Ajax
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.concurrent.Future

object Api {

  def getProcessIndex(): Future[Set[String]] = Ajax.get("/process_topology/_index").map { xhr =>
    upickle.default.read[Set[String]](xhr.responseText)
  }

  def getProcess(id: String): Future[PetriNetModel] = Ajax.get(s"/process_topology/by_id/$id").map { xhr =>
    upickle.default.read[PetriNetModel](xhr.responseText)
  }

  def getProcessState(name: String, id: String): Future[ProcessState] = ???
}
