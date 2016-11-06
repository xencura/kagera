package io.kagera

import cats.data.State
import fs2.{ Strategy, Task }
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.execution.EventSourcing._

import scala.collection.Set

package object execution {

  /**
   * Fires a specific transition with input, computes the marking it should consume
   */
  def fireTransition[S, E](
    transition: Transition[Any, E, S],
    input: Any
  ): State[Instance[S], Either[String, Job[S, E]]] =
    State { instance =>
      instance.isBlockedReason(transition.id) match {
        case Some(reason) =>
          (instance, Left(reason))
        case None =>
          instance.process.enabledParameters(instance.availableMarking).get(transition) match {
            case None =>
              (instance, Left(s"Not enough consumable tokens"))
            case Some(params) =>
              val (updatedState, job) = createJob(transition, params.head, input)(instance)
              (updatedState, Right(job))
          }
      }
    }

  /**
   * Creates a job for a specific input & marking. Does not do any validation on the parameters
   */
  def createJob[E, S](
    transition: Transition[Any, E, S],
    consume: Marking,
    input: Any
  ): Instance[S] => (Instance[S], Job[S, E]) = s => {
    val job = Job[S, E](s.nextJobId(), s.process, s.state, transition, consume, input)
    val newState = s.copy(jobs = s.jobs + (job.id -> job))
    (newState, job)
  }

  /**
   * Finds the (optional) first transition that is automated & enabled
   */
  def firstFirstEnabled[S]: State[Instance[S], Option[Job[S, _]]] = State { instance =>
    instance.process
      .enabledParameters(instance.availableMarking)
      .find { case (t, markings) =>
        t.isAutomated && !instance.isBlockedReason(t.id).isDefined
      }
      .map { case (t, markings) =>
        val job = Job[S, Any](
          instance.nextJobId(),
          instance.process,
          instance.state,
          t.asInstanceOf[Transition[Any, Any, S]],
          markings.head,
          ()
        )
        (instance.copy(jobs = instance.jobs + (job.id -> job)), Some(job))
      }
      .getOrElse((instance, None))
  }

  def fireTransitionById[S](id: Long, input: Any): State[Instance[S], Either[String, Job[S, Any]]] =
    State
      .inspect[Instance[S], Option[Transition[Any, Any, S]]] { instance =>
        instance.process.transitions.findById(id).map(_.asInstanceOf[Transition[Any, Any, S]])
      }
      .flatMap {
        case None => State.pure(Left(s"No transition exists with id $id"))
        case Some(t) => fireTransition(t, input)
      }

  /**
   * Finds all automated enabled transitions.
   */
  def fireAllEnabledTransitions[S]: State[Instance[S], Set[Job[S, _]]] =
    firstFirstEnabled[S].flatMap {
      case None => State.pure(Set.empty)
      case Some(job) => fireAllEnabledTransitions[S].map(_ + job)
    }

  /**
   * Executes a job returning a TransitionEvent
   */
  def runJob[S, E](job: Job[S, E], executor: TransitionExecutor[S])(implicit S: Strategy): Task[TransitionEvent] = {
    val startTime = System.currentTimeMillis()

    executor
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
