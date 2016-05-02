package io.kagera

import scala.concurrent.Future
import scalaz.@@

package object api {

  type Marking[P] = Map[P, Long]

  object tags {
    trait Id
    trait Weight
    trait Label
  }

  // TODO decide, shapeless or scalaz tags?
  type Identifiable[T] = T => Long @@ tags.Id
  type Labeled[T] = T => String @@ tags.Label

  implicit class LabeledFn[T : Labeled](seq: Iterable[T]) {
    def findByLabel(label: String) = seq.find(e => implicitly[Labeled[T]].apply(e) == label)
  }

  implicit class IdFn[T : Identifiable](seq: Iterable[T]) {
    def findById(id: String) = seq.find(e => implicitly[Identifiable[T]].apply(e) == id)
  }

  /**
   * Type alias for a petri net with token game and executor. This makes an executable process.
   *
   * @tparam P
   *   The place type
   * @tparam T
   *   The transition type
   * @tparam M
   *   The marking type
   */
  trait PTProcess[P, T, M] extends PetriNet[P, T] with TokenGame[P, T, M] with TransitionExecutor[P, T, M]

  /**
   * Type class for marking 'like' semantics.
   */
  trait MarkingLike[M, P] {

    /**
     * Returns an empty instance of this marking type.
     *
     * @return
     *   The empty marking.
     */
    def emptyMarking: M

    /**
     * Returns the multiplicity of the marking, that is: A map from place to the nr of tokens in that place
     *
     * @param marking
     *
     * @return
     *   The empty marking.
     */
    def multiplicity(marking: M): Marking[P]

    def isSubMarking(m: M, other: M): Boolean

    def consume(from: M, other: M): M

    def produce(into: M, other: M): M
  }

  implicit class MarkingLikeApi[M, P](val m: M)(implicit val markingLike: MarkingLike[M, P]) {

    def multiplicity = markingLike.multiplicity(m)

    def consume(other: M) = markingLike.consume(m, other)

    def produce(other: M) = markingLike.produce(m, other)

    def isEmpty() = markingLike.multiplicity(m).isEmpty

    def isSubMarking(other: M) = markingLike.isSubMarking(m, other)
  }

  trait TransitionExecutor[P, T, M] {

    this: PetriNet[P, T] =>

    def fireTransition(marking: M)(transition: T): Future[M]
  }
}
