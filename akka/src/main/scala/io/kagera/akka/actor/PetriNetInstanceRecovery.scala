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

package io.kagera.akka.actor

import akka.actor.ActorSystem
import akka.persistence.{ PersistentActor, RecoveryCompleted }
import io.kagera.api.colored.ExecutablePetriNet
import io.kagera.execution.EventSourcing._
import io.kagera.execution.{ EventSourcing, Instance }
import io.kagera.persistence.{ messages, Serialization }

trait PetriNetInstanceRecovery[S] {

  this: PersistentActor =>

  def topology: ExecutablePetriNet[S]

  implicit val system: ActorSystem = context.system
  val serializer = new Serialization(new AkkaObjectSerializer(context.system))

  def onRecoveryCompleted(state: Instance[S]): Unit

  def applyEvent(i: Instance[S])(e: Event): Instance[S] = EventSourcing.applyEvent(e).runS(i).value

  def persistEvent[T, E <: Event](instance: Instance[S], e: E)(fn: E => T): Unit = {
    val serializedEvent = serializer.serializeEvent(e)(instance)
    persist(serializedEvent) { _ => fn.apply(e) }
  }

  private var recoveringState: Instance[S] = Instance.uninitialized[S](topology)

  private def applyToRecoveringState(e: AnyRef): Unit = {
    val deserializedEvent = serializer.deserializeEvent(e)(recoveringState)
    recoveringState = applyEvent(recoveringState)(deserializedEvent)
  }

  override def receiveRecover: Receive = {
    case e: messages.Initialized => applyToRecoveringState(e)
    case e: messages.TransitionFired => applyToRecoveringState(e)
    case e: messages.TransitionFailed => applyToRecoveringState(e)
    case RecoveryCompleted =>
      if (recoveringState.sequenceNr > 0)
        onRecoveryCompleted(recoveringState)
  }
}
