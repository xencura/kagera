package io.kagera

import io.kagera.api.ScalaGraph.BiPartiteGraph

import scalaz.@@

package object api {

  type Marking[P] = Map[P, Long]

  object tags {
    trait Id
    trait Weight
    trait Label
  }

  type Identifiable[T] = T => Long @@ tags.Id
  type Labeled[T] = T => String @@ tags.Label

  type PTProcess[P, T, M] = PetriNet[P, T] with TokenGame[P, T, M] with TransitionExecutor[P, T, M]

  // given a process and current marking picks the next transition and marking to fire
  type Step[P, T, M] = (PTProcess[P, T, M], M) => Option[(T, M)]

  // TODO move these stepping algorithms

  def stepFirst[P, T, M]: Step[P, T, M] = (process, marking) => {
    process.enabledParameters(marking).headOption.map { case (t, enabledMarkings) => (t, enabledMarkings.head) }
  }

  def stepRandom[P, T, M]: Step[P, T, M] = (process, marking) => {
    import scalaz.syntax.std.boolean._
    import scala.util.Random

    val params = process.enabledParameters(marking)

    params.nonEmpty.option {
      val n = Random.nextInt(Math.min(10, params.size))
      val (t, enabledMarkings) = Stream.continually(params.toStream).flatten.apply(n)
      (t, enabledMarkings.head)
    }
  }

  /**
   * Type class for marking 'like' semantics.
   */
  trait MarkingLike[M, P] {

    def emptyMarking: M

    def tokenCount(marking: M): Marking[P]

    def isSubMarking(m: M, other: M): Boolean

    def consume(from: M, other: M): M

    def produce(into: M, other: M): M
  }

  implicit class MarkingLikeApi[M, P](val m: M)(implicit val markingLike: MarkingLike[M, P]) {
    def consume(other: M) = markingLike.consume(m, other)
    def produce(other: M) = markingLike.produce(m, other)
    def isEmpty() = markingLike.tokenCount(m).isEmpty
    def isSubMarking(other: M) = markingLike.isSubMarking(m, other)
  }

  trait PetriNet[P, T] {

    type Node = Either[P, T]

    def innerGraph: BiPartiteGraph[P, T]

    def places: Set[P]
    def transitions: Set[T]

    def inMarking(t: T): Marking[P]
    def outMarking(t: T): Marking[P]

    def nodes: scala.collection.Set[Node]
  }

  trait TransitionExecutor[P, T, M] {

    this: PetriNet[P, T] =>

    def fireTransition(consume: M)(transition: T): M
  }

  trait TokenGame[P, T, M] {

    this: PetriNet[P, T] =>

    def enabledParameters(m: M): Map[T, Iterable[M]] = {
      // inefficient, fix
      enabledTransitions(m).view.map(t => t -> consumableMarkings(m)(t)).toMap
    }

    def consumableMarkings(m: M)(t: T): Iterable[M]

    // horribly inefficient, fix
    def isEnabled(marking: M)(t: T): Boolean = enabledTransitions(marking).contains(t)

    def enabledTransitions(marking: M): Set[T]
  }
}
