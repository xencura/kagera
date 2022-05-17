/*
 * Copyright (c) 2022 Xencura GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.kagera.akka

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import io.kagera.akka.actor.PetriNetInstanceApi
import io.kagera.akka.actor.PetriNetInstanceProtocol._
import org.scalatest.matchers.should.Matchers._

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

    "Return an empty source when the petri net instance is 'uninitialized'" in new TestSequenceNet {

      val waitTimeout = 2 seconds

      override val sequence = Seq(transition()(_ => Added(1)))

      val actor = PetriNetInstanceSpec.createPetriNetActor[Set[Int]](petriNet)
      val api = new PetriNetInstanceApi(petriNet, actor)
      val source: Source[TransitionResponse, NotUsed] = api.askAndCollectAll(FireTransition(1, ()))

      val responses = Await.result(source.runWith(Sink.seq[TransitionResponse]), waitTimeout)

      responses.isEmpty shouldBe true
    }
  }
}
