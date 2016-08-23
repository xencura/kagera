package io.kagera.akka.actor

import io.kagera.akka.actor.PersistentPetriNetActor._
import io.kagera.api.colored._

/**
 * Translates to/from the persist and internal event model
 *
 * @tparam S
 * @tparam E
 *   The event type
 */
trait TransitionEventAdapter[S, E] {

  def writeEvent(e: TransitionFired): E

  def readEvent(process: ColoredPetriNetProcess[S], currentMarking: ColoredMarking, e: E): TransitionFired
}
