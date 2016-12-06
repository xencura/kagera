package io.kagera.akka

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import io.kagera.akka.actor.PetriNetInstanceApi._

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import org.scalatest.Matchers._

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

      val source: Source[TransitionResponse, NotUsed] =
        actor.fireAndCollectAll(petriNet, FireTransition(1, ()))(Timeout(waitTimeout))
      val responses = Await.result(source.runWith(Sink.seq[TransitionResponse]), waitTimeout)

      responses.size shouldBe 3

      responses(0).transitionId shouldBe 1
      responses(1).transitionId shouldBe 2
      responses(2).transitionId shouldBe 3
    }
  }
}
