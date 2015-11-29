package io.process.statebox.process

import org.slf4j.LoggerFactory

package object simple {

  type Marking[P] = Map[P, Int]

  implicit class MarkingFunctions[P](marking: Marking[P]) {
    def consume(other: Marking[P]) = {

      other.foldLeft(marking) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => throw new IllegalStateException(s"No such place in marking: $p")
          case Some(count) if count < amount => throw new IllegalStateException(s"Too few tokens in place: $p")
          case Some(count) if count == amount => m - p
          case Some(count) => m + (p -> (count - amount))
        }
      }
    }

    def produce(other: Marking[P]) = {
      other.foldLeft(marking) { case (m, (p, amount)) =>
        m.get(p) match {
          case None => m + (p -> amount)
          case Some(count) => m + (p -> (count + amount))
        }
      }
    }
  }

  trait SimpleExecutor[P, T] extends TransitionExecutor[P, T, Marking[P]] {

    this: PetriNet[P, T] =>

    val log = LoggerFactory.getLogger(classOf[SimpleExecutor[_, _]])

    override def fireTransition(marking: Marking[P])(transition: T, consume: Marking[P]): Marking[P] = {
      log.debug("Firing transition {}", transition)

      val in = inMarking(transition)
      log.debug("inMarking: {}", in)

      val out = outMarking(transition)
      log.debug("outMarking: {}", out)

      val newMarking = marking.consume(consume).produce(out)

      log.info("fired transition {}, result: {}", transition, newMarking)

      newMarking
    }
  }
}
