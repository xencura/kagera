package io.kagera

import fs2.{ Strategy, Task }
import io.kagera.api.colored.{ Marking, Transition }
import io.kagera.execution.EventSourcing.{ TransitionEvent, TransitionFailedEvent, TransitionFiredEvent }

import scala.collection.Set

package object execution {

  type InstanceState[S, T] = Instance[S] => (Instance[S], T)

  /**
   * Fires a specific transition with input, computes the marking it should consume
   */
  def fireTransition[E, S](transition: Transition[Any, E, S], input: Any): InstanceState[S, Either[Job[S, E], String]] =
    s => {
      s.isBlockedReason(transition.id) match {
        case Some(reason) =>
          (s, Right(reason))
        case None =>
          s.process.enabledParameters(s.availableMarking).get(transition) match {
            case None =>
              (s, Right(s"Not enough consumable tokens"))
            case Some(params) =>
              val (updatedState, job) = fireUnsafe(transition, params.head, input)(s)
              (updatedState, Left(job))
          }
      }
    }

  /**
   * Creates a job for a specific input & marking. Does not do any validation on the parameters
   */
  def fireUnsafe[E, S](transition: Transition[Any, E, S], consume: Marking, input: Any): InstanceState[S, Job[S, E]] =
    s => {
      val job = Job[S, E](s.nextJobId(), s.process, s.state, transition, consume, input)
      val newState = s.copy(jobs = s.jobs + (job.id -> job))
      (newState, job)
    }

  def fireAllEnabledTransitions[S]: InstanceState[S, Set[Job[S, _]]] = s => {
    val enabled = s.process.enabledParameters(s.availableMarking).find { case (t, markings) =>
      t.isAutomated && !s.isBlockedReason(t.id).isDefined
    }

    enabled.headOption
      .map { case (t, markings) =>
        val (newState, job) = fireUnsafe(t.asInstanceOf[Transition[Any, Any, S]], markings.head, ())(s)
        fireAllEnabledTransitions(newState) match { case (state, jobs) => (state, jobs + job) }
      }
      .getOrElse((s, Set.empty[Job[S, _]]))
  }

  /**
   * Executes a job returning a TransitionEvent
   */
  def runJob[S, E](job: Job[S, E])(implicit S: Strategy): Task[TransitionEvent] = {
    val startTime = System.currentTimeMillis()

    job.process
      .fireTransition(job.transition)(job.consume, job.processState, job.input)
      .map { case (produced, out) =>
        TransitionFiredEvent(
          job.id,
          job.transition.id,
          startTime,
          System.currentTimeMillis(),
          job.consume,
          produced,
          out
        )
      }
      .handle { case e: Throwable =>
        val failureCount = job.failureCount + 1
        val failureStrategy = job.transition.exceptionStrategy(e, failureCount)
        TransitionFailedEvent(
          job.id,
          job.transition.id,
          startTime,
          System.currentTimeMillis(),
          job.consume,
          job.input,
          e.getCause.getMessage,
          failureStrategy
        )
      }
      .async
  }
}
