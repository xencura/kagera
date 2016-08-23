package io.kagera.akka.actor

import io.kagera.akka.actor.DefaultEventAdapter._
import io.kagera.akka.actor.PersistentPetriNetActor.TransitionFired
import io.kagera.api.colored.{ ColoredMarking, _ }
import io.kagera.api.multiset._
import io.kagera.api._

object DefaultEventAdapter {

  // map from place id -> token id's
  type MarkingIndex = Map[Long, MultiSet[Int]]

  // persist model
  case class TransitionFiredPersist(
    transition_id: Long,
    time_started: Long,
    time_completed: Long,
    consumed: MarkingIndex,
    produced: Map[Long, MultiSet[_]],
    out: Any
  ) // this should be Array[Byte] with pluggable

  implicit class ColoredMarkingFns(marking: ColoredMarking) {
    def indexed: Map[Long, MultiSet[Int]] = marking.data.map { case (place, tokens) =>
      place.id -> tokens.map { case (value, count) =>
        tokenIdentifier(place)(value) -> count
      }
    }
  }

  implicit class MarkingIndexFns(indexedMarking: MarkingIndex) {
    def realizeFrom(marking: ColoredMarking): ColoredMarking = {
      indexedMarking.map { case (pid, values) =>
        val place = marking.markedPlaces.getById(pid)
        val tokens = values.map { case (id, count) =>
          val value = marking(place).keySet.find(e => tokenIdentifier(place)(e) == id).get
          value -> count
        }

        place -> tokens
      }.toMarking
    }
  }

  // this approach is fragile, the function cannot change ever or recovery breaks
  // a more robust alternative is to generate the ids and persist them
  def tokenIdentifier[C](p: Place[C]): Any => Int = obj => hashCodeOf[Any](obj)

  def hashCodeOf[T](e: T): Int = {
    if (e == null)
      -1
    else
      e.hashCode()
  }
}

trait DefaultEventAdapter[S] extends TransitionEventAdapter[S, TransitionFiredPersist] {

  override def writeEvent(e: TransitionFired): TransitionFiredPersist = {
    val consumedIndex: Map[Long, MultiSet[Int]] = e.consumed.indexed
    val produceIndex: Map[Long, MultiSet[_]] = e.produced.data.map { case (place, tokens) => place.id -> tokens }.toMap

    TransitionFiredPersist(e.transition_id, e.time_started, e.time_completed, consumedIndex, produceIndex, e.out)
  }

  override def readEvent(
    process: ColoredPetriNetProcess[S],
    currentMarking: ColoredMarking,
    e: TransitionFiredPersist
  ): TransitionFired = {
    val transition = process.getTransitionById(e.transition_id)
    val consumed = e.consumed.realizeFrom(currentMarking)
    val produced = ColoredMarking(data = e.produced.map { case (id, tokens) =>
      process.places.getById(id) -> tokens
    }.toMap)
    TransitionFired(transition, e.time_started, e.time_started, consumed, produced, e.out)
  }
}
