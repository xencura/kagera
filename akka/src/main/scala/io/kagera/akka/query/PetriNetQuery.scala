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

package io.kagera.akka.query

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.scaladsl._
import akka.stream.scaladsl._
import io.kagera.akka.actor.{ AkkaObjectSerializer, PetriNetInstance }
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.execution.EventSourcing._
import io.kagera.execution.Instance
import io.kagera.persistence.Serialization

trait PetriNetQuery[S] {

  def readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery

  def eventsForInstance(instanceId: String, topology: ExecutablePetriNet[S])(implicit
    actorSystem: ActorSystem
  ): (Source[(Instance[S], Event), NotUsed]) = {

    val serializer = new Serialization(new AkkaObjectSerializer(actorSystem))

    val persistentId = PetriNetInstance.petriNetInstancePersistenceId(instanceId)
    val src = readJournal.currentEventsByPersistenceId(persistentId, 0, Long.MaxValue)

    src
      .scan[(Instance[S], Event)]((Instance.uninitialized(topology), null.asInstanceOf[Event])) {
        case ((instance, prev), e) =>
          val event = e.event.asInstanceOf[AnyRef]
          val deserializedEvent = serializer.deserializeEvent(event)(instance)
          val updatedInstance = applyEvent(deserializedEvent).runS(instance).value
          (updatedInstance, deserializedEvent)
      }
      .drop(1)
  }
}
