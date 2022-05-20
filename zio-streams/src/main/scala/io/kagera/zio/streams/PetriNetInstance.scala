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

package io.kagera.zio.streams

import io.kagera.api.colored._
import io.kagera.api.placeToNode
import io.kagera.execution.EventSourcing._
import io.kagera.execution._
import io.kagera.zio.streams.PetriNetInstance.{ defaultSettings, Settings }
import io.kagera.zio.streams.PetriNetInstanceProtocol._
import zio.Clock
import zio.interop.catz._
import zio.{ RIO, Task, UIO, ZIO }
import zio.stream._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.{ existentials, postfixOps }

object PetriNetInstance {

  case class Settings(evaluationStrategy: ExecutionContext, idleTTL: Option[FiniteDuration])

  val defaultSettings: Settings =
    Settings(evaluationStrategy = ExecutionContext.Implicits.global, idleTTL = Some(5 minutes))

  def petriNetInstancePersistenceId(processId: String): String = s"process-$processId"

  def instanceState[S](instance: Instance[S]): StateResponse[S] = {
    val failures = instance.failedJobs.map { e =>
      e.transitionId -> PetriNetInstanceProtocol.ExceptionState(
        e.consecutiveFailureCount,
        e.failureReason,
        e.failureStrategy
      )
    }.toMap

    StateResponse[S](
      instance.sequenceNr,
      instance.marking,
      Option(instance.state),
      failures,
      instance.enabledTransitions
    )
  }
}
class PetriNetInstance[S](executor: TransitionExecutor[Task, S]) {

  def props[S](topology: ExecutablePetriNet[S], settings: Settings = defaultSettings) = {
    type State = Instance[S]
    ZPipeline.mapAccumZIO[Any, Throwable, Message[_], State, Response](Instance.uninitialized[S](topology)) {
      case (state, msg) =>
        lazy val instance = state.getOrElse(Instance.uninitialized[S](topology))
        msg match {
          case in: SetMarkingAndState[S] =>
            val newState: State = state.copy(state = Some(in.state), marking = in.marking, sequenceNr = 1)
            ZIO.succeed(newState -> Initialized(newState.marking, newState.state))

          case AssignTokenToPlace(token, place) =>
            val newState = instanceState[S](instance)
            ZIO.succeed((??? : State, ??? : Response)) // TODO Implement real functionality
          case UpdateTokenInPlace(from, to, place) =>
            step(state.copy(marking = state.marking.updateIn(place, from, to))).map(instance =>
              (state, instanceState(state))
            )
          case msg @ FireTransition(id, input, correlationId) =>
            fireTransitionById[S](id, input).run(instance).value match {
              case (updatedInstance, Right(job)) =>
                runJobAsync[Task, S, Any](job, state.executor).map {
                  case ev: TransitionFiredEvent =>
                    zio.stream.Stream(
                      TransitionFired[S](ev.transitionId, ev.consumed, ev.produced, newState.map(instanceState).get)
                    )
                  case e @ TransitionFailedEvent(jobId, transitionId, _, _, consume, input, reason, strategy) =>
                    strategy match {
                      /* TODO
                  val updatedInstance = applyEvent(instance)(e)
                  case RetryWithDelay(delay) =>
                    log.warning(
                      s"Scheduling a retry of transition '${topology.transitions.getById(transitionId)}' in $delay milliseconds"
                    )
                    val originalSender = sender()
                    system.scheduler.scheduleOnce(delay milliseconds) {
                      runJobAsync(updatedInstance.jobs(jobId), executor)
                    }
                    updateAndRespond(applyEvent(instance)(e))
                       */
                      case _ =>
                        // persistEvent(instance, e)((applyEvent(instance) _).andThen(updateAndRespond _))
                        zio.stream.Stream(
                          TransitionFailed(transitionId, consume, input, reason, strategy): TransitionResponse
                        )
                    }
                }
              case (_, Left(reason)) =>
                ZIO.succeed(zio.stream.Stream(TransitionNotEnabled(id, reason)))
            }
        }
    }
    // TODO remove side effecting here
    def step(instance: Instance[S]): Task[Instance[S]] = {
      fireAllEnabledTransitions.run(instance).value match {
        case (updatedInstance, jobs) =>
          if (jobs.isEmpty && updatedInstance.activeJobs.isEmpty) {
            /* TODO: Needed?
          settings.idleTTL.foreach { ttl =>
            log.debug("Process has no running jobs, killing the streams in: {}", ttl)
            system.scheduler.scheduleOnce(ttl, context.self, PoisonPill)
          }
             */
          }

          ZIO
            .foreach(jobs.toSeq)(job => runJobAsync(job, executor))
            .map(_.foldLeft(updatedInstance)(applyEvent(_)(_)))
      }
    }
  }
}
