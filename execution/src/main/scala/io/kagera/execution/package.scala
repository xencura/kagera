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

package io.kagera

import java.io.{ PrintWriter, StringWriter }

import cats.ApplicativeError
import cats.data.State
import cats.syntax.all._
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.execution.EventSourcing.{ TransitionEvent, _ }

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
          instance.enabledParameters.get(transition) match {
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
    val job = Job[S, E](s.nextJobId(), s.state, transition, consume, input)
    val newState = s.copy(jobs = s.jobs + (job.id -> job))
    (newState, job)
  }

  /**
   * Finds the (optional) first transition that is automated & enabled
   */
  def fireFirstEnabled[S]: State[Instance[S], Option[Job[S, _]]] = State { instance =>
    instance.enabledParameters
      .find { case (t, _) =>
        t.isAutomated && instance.isBlockedReason(t.id).isEmpty
      }
      .map { case (t, markings) =>
        val job =
          Job[S, Any](instance.nextJobId(), instance.state, t.asInstanceOf[Transition[Any, Any, S]], markings.head, ())
        (instance.copy(jobs = instance.jobs + (job.id -> job)), Some(job))
      }
      .getOrElse((instance, None))
  }

  def fireTransitionById[S](id: Long, input: Any): State[Instance[S], Either[String, Job[S, Any]]] =
    State
      .inspect[Instance[S], Option[Transition[Any, Any, S]]](
        _.transitionById(id).map(_.asInstanceOf[Transition[Any, Any, S]])
      )
      .flatMap {
        case None => State.pure(Left(s"No transition exists with id $id"))
        case Some(t) => fireTransition(t, input)
      }

  /**
   * Finds all automated enabled transitions.
   */
  def fireAllEnabledTransitions[S]: State[Instance[S], Set[Job[S, _]]] =
    fireFirstEnabled[S].flatMap {
      case None => State.pure(Set.empty)
      case Some(job) => fireAllEnabledTransitions[S].map(_ + job)
    }

  /**
   * Executes a job returning a Task[TransitionEvent]
   */
  def runJobAsync[F[_], S, E](job: Job[S, E], executor: TransitionExecutor[F, S])(implicit
    applicativeError: ApplicativeError[F, Throwable]
  ): F[TransitionEvent] = {
    val startTime = System.currentTimeMillis()

    val transitionFunction: TransitionFunction[F, Any, E, S] = executor
      .fireTransition(job.transition)
    val transitionApplied: F[(Marking, E)] = transitionFunction(job.consume, job.processState, job.input)
    transitionApplied
      .map { case (produced, out) =>
        TransitionFiredEvent(
          job.id,
          job.transition.id,
          startTime,
          System.currentTimeMillis(),
          job.consume,
          produced,
          Some(out)
        ): TransitionEvent
      }
      .handleErrorWith { e: Throwable =>
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        val stackTraceString = sw.toString

        applicativeError.pure(
          TransitionFailedEvent(
            job.id,
            job.transition.id,
            startTime,
            System.currentTimeMillis(),
            job.consume,
            Some(job.input),
            stackTraceString,
            job.transition.exceptionStrategy(e, job.failureCount + 1)
          )
        )
      }
  }
}
