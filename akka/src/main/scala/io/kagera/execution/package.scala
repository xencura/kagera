package io.kagera

import java.io.{ PrintWriter, StringWriter }

import cats.data.State
import cats.effect.IO
import io.kagera.api._
import io.kagera.api.colored._
import io.kagera.execution.EventSourcing._

import scala.collection.Set
import scala.concurrent.ExecutionContext

package object execution {

  /**
   * Fires a specific transition with input, computes the marking it should consume
   */
  def fireTransition[S, T <: Transition[Any, _, S]](
    transition: T,
    input: Any
  ): State[Instance[S, T], Either[String, Job[S, T]]] =
    State { instance =>
      instance.isBlockedReason(transition.id) match {
        case Some(reason) =>
          (instance, Left(reason))
        case None =>
          instance.process.enabledParameters(instance.availableMarking).get(transition) match {
            case None =>
              (instance, Left(s"Not enough consumable tokens"))
            case Some(params) =>
              val (updatedState, job) = createJob[S, T](transition, params.head, input)(instance)
              (updatedState, Right(job))
          }
      }
    }

  /**
   * Creates a job for a specific input & marking. Does not do any validation on the parameters
   */
  def createJob[S, T <: Transition[Any, _, S]](
    transition: T,
    consume: Marking,
    input: Any
  ): Instance[S, T] => (Instance[S, T], Job[S, T]) = s => {
    val job = Job[S, T](s.nextJobId(), s.state, transition, consume, input)
    val newState = s.copy(jobs = s.jobs + (job.id -> job))
    (newState, job)
  }

  /**
   * Finds the (optional) first transition that is automated & enabled
   */
  def fireFirstEnabled[S, T <: Transition[Any, _, S]]: State[Instance[S, T], Option[Job[S, T]]] = State { instance =>
    instance.process
      .enabledParameters(instance.availableMarking)
      .find { case (t, markings) =>
        t.isAutomated && !instance.isBlockedReason(t.id).isDefined
      }
      .map { case (t, markings) =>
        val job = Job[S, T](instance.nextJobId(), instance.state, t, markings.head, ())
        (instance.copy(jobs = instance.jobs + (job.id -> job)), Some(job))
      }
      .getOrElse((instance, None))
  }

  def fireTransitionById[S, T <: Transition[Any, _, S]](
    id: Long,
    input: Any
  ): State[Instance[S, T], Either[String, Job[S, T]]] =
    State
      .inspect[Instance[S, T], Option[T]] { instance =>
        instance.process.transitions.findById(id)
      }
      .flatMap {
        case None => State.pure(Left(s"No transition exists with id $id"))
        case Some(t) => fireTransition[S, T](t, input)
      }

  /**
   * Finds all automated enabled transitions.
   */
  def fireAllEnabledTransitions[S, T <: Transition[Any, _, S]]: State[Instance[S, T], Set[Job[S, T]]] =
    fireFirstEnabled[S, T].flatMap {
      case None => State.pure(Set.empty)
      case Some(job) => fireAllEnabledTransitions[S, T].map(_ union Set(job))
    }

  /**
   * Executes a job returning a Task[TransitionEvent]
   */
  def runJobAsync[S, T <: Transition[_, _, S]](job: Job[S, T], executor: TransitionExecutor[IO, T])(implicit
    S: ExecutionContext,
    executorFactory: TransitionExecutorFactory.WithInputOutputState[IO, T, Any, _, S]
  ): IO[TransitionEvent] = {
    val startTime = System.currentTimeMillis()

    executor
      .fireTransition(job.transition)
      .apply(job.consume, job.processState, job.input)
      .map { case (produced, out) =>
        TransitionFiredEvent(
          job.id,
          job.transition.id,
          startTime,
          System.currentTimeMillis(),
          job.consume,
          produced,
          Some(out)
        )
      }
      .handleErrorWith { case e: Throwable =>
        val failureCount = job.failureCount + 1
        val failureStrategy = job.transition.exceptionStrategy(e, failureCount)

        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        val stackTraceString = sw.toString

        IO(
          TransitionFailedEvent(
            job.id,
            job.transition.id,
            startTime,
            System.currentTimeMillis(),
            job.consume,
            Some(job.input),
            stackTraceString,
            failureStrategy
          )
        )
      }
  }
}
