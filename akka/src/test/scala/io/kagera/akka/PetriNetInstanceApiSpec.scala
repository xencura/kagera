package io.kagera.akka

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import io.kagera.akka.actor.PetriNetInstanceApi
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import org.scalatest.Matchers._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

class PetriNetInstanceApiSpec extends AkkaTestBase {

  implicit def materializer = ActorMaterializer()
  implicit def ec: ExecutionContext = system.dispatcher

  "The PetriNetInstanceApi" should {

    "Return a source of events resulting from a TransitionFired command" in new TestSequenceNet {

      val waitTimeout = 2 seconds

      override val sequence = Seq(
        transition()(_ => Added(1)),
        transition(automated = true)(_ => Added(2)),
        transition(automated = true)(_ => Added(3))
      )

      val actor = PetriNetInstanceSpec.createPetriNetActor[Set[Int]](petriNet)

      actor ! Initialize(initialMarking, Set.empty)
      expectMsgClass(classOf[Initialized[_]])

      val api = new PetriNetInstanceApi(petriNet, actor)
      val source: Source[TransitionResponse, NotUsed] = api.askAndCollectAll(FireTransition(1, ()))
      val responses = Await.result(source.runWith(Sink.seq[TransitionResponse]), waitTimeout)

      responses.size shouldBe 3

      responses(0).transitionId shouldBe 1
      responses(1).transitionId shouldBe 2
      responses(2).transitionId shouldBe 3
    }
  }
}
