package io.kagera.demo.http

import akka.http.scaladsl.common.{ EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshalling.{ Marshaller, Marshalling }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.util.{ ByteString, Timeout }
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import io.kagera.akka.actor.PetriNetInstance
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.api.colored.{ ExecutablePetriNet, Generators, Marking, Transition }
import io.kagera.demo.{ ConfiguredActorSystem, Queries }
import upickle.default._

trait Routes extends Directives with Queries with UpickleSupport {

  this: ConfiguredActorSystem =>

  import scala.concurrent.duration._

  implicit val streamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
  implicit val timeout = Timeout(2 seconds)

  implicit val stringMarshaller = Marshaller.strict[String, ByteString] { t =>
    Marshalling.WithFixedContentType(ContentTypes.`application/json`, () => {
      ByteString(s""""$t"""")
    })
  }

  val repository: Map[String, ExecutablePetriNet[_, Transition[_, _, _]]] = Map(
    "test" -> Generators.Uncolored.sequence(5))

  val indexRoute = path("index.html") {
    get {
      complete {
        HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, ByteString(StaticPages.index.render)))
      }
    }
  }

  val topologyRoutes = pathPrefix("process_topology") {
    path("index") {
      complete(upickle.default.write(repository.keySet))
    } ~
      path("by_id" / Segment) { id =>
        get {
          repository.get(id) match {
            case None => complete(StatusCodes.NotFound -> s"no such process: $id")
            case Some(process) => complete(upickle.default.write(Util.toModel[Transition[_, _, _]](process)))
          }
        }
      }
  }

  val resourceRoutes = pathPrefix("resources") { getFromResourceDirectory("") }

  def actorForProcess(id: String) = system.actorSelection(s"/user/$id")

  val processRoutes = pathPrefix("process") {

    path("index") {
      get {
        complete(allProcessIds)
      }
    } ~
      path("create" / Segment) { topologyId =>
        post {
          val topology = repository(topologyId).asInstanceOf[ExecutablePetriNet[Unit, _]]
          val id = java.util.UUID.randomUUID.toString
          //          system.actorOf(PetriNetInstance.props(topology, Marking.empty, ()), id)
          complete(id)
        }
      } ~
      pathPrefix("by_id" / Segment) { id =>

        val processActor = actorForProcess(id)

        pathEndOrSingleSlash {
          get {
            // should return the current state (marking) of the process
            val futureResult = processActor.ask(GetState).mapTo[InstanceState[_]].map {
              state => state.marking.toString
            }
            complete(futureResult)
          }
        } ~ path("journal") {
          get {
            val journal = journalFor(id)
            complete(journal)
          }
        } ~ path("fire" / Segment) { tid =>
          post {
            val msg = FireTransition(tid.toLong, ())
            val futureResult = processActor.ask(msg).mapTo[TransitionResponse].map {
              case success: TransitionFired[_] => "success"
              case failure: TransitionFailed => "failure"
              case notEnabled: TransitionNotEnabled => "not enabled"
            }
            complete(futureResult)
          }
        }
      }
  }
}