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

package io.kagera.zio.actor

import io.kagera.api.colored._
import io.kagera.api.placeToNode
import io.kagera.execution.EventSourcing._
import io.kagera.execution._
import io.kagera.zio.actor.PetriNetInstance.Settings
import io.kagera.zio.actor.PetriNetInstanceProtocol._
import zio.actors.persistence.PersistenceId.PersistenceId
import zio.actors.persistence.{ Command, EventSourcedStateful }
import zio.actors.{ ActorRef, Context }
import zio.clock.Clock
import zio.interop.catz._
import zio.{ RIO, Task, UIO, ZIO }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.existentials

object PetriNetInstance {

  case class Settings(evaluationStrategy: ExecutionContext, idleTTL: Option[FiniteDuration])

  val defaultSettings: Settings =
    Settings(evaluationStrategy = ExecutionContext.Implicits.global, idleTTL = Some(5 minutes))

  def petriNetInstancePersistenceId(processId: String): String = s"process-$processId"

  def instanceState[S](instance: Instance[S]): InstanceState[S] = {
    val failures = instance.failedJobs.map { e =>
      e.transitionId -> PetriNetInstanceProtocol.ExceptionState(
        e.consecutiveFailureCount,
        e.failureReason,
        e.failureStrategy
      )
    }.toMap

    InstanceState[S](
      instance.sequenceNr,
      instance.marking,
      Option(instance.state),
      failures,
      instance.enabledTransitions
    )
  }

  def props[S](topology: ExecutablePetriNet[S], persistenceId: PersistenceId, settings: Settings = defaultSettings) = {
    implicit val runtime: zio.Runtime[zio.ZEnv] = zio.Runtime.default
    new PetriNetInstance[S](topology, settings, new TransitionExecutorImpl[Task, S](topology), persistenceId)
  }
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetInstance[S](
  val topology: ExecutablePetriNet[S],
  val settings: Settings,
  executor: TransitionExecutor[Task, S],
  persistenceId: PersistenceId
) extends EventSourcedStateful[Clock, Option[Instance[S]], Message, Event](persistenceId) {
  import PetriNetInstance._
  type State = Option[Instance[S]]
  override def receive[A](state: State, msg: Message[A], context: Context): RIO[Any, (Command[Event], State => A)] = {
    lazy val instance = state.getOrElse(Instance.uninitialized(topology))
    msg match {
      case in: SetMarkingAndState[S] =>
        UIO(
          (
            Command.persist(InitializedEvent(in.marking, in.state)),
            (s: State) => s.map(i => Initialized(i.marking, i.state)).get: Response
          )
        )

      case AssignTokenToPlace(token, place) =>
        UIO((Command.ignore, _ => instanceState(instance))) // TODO Implement real functionality
      case UpdateTokenInPlace(from, to, place) =>
        step(state.get.copy(marking = state.get.marking.updateIn(place, from, to))).map(instance =>
          (Command.persist(UpdatedInstance(instance)), state => instanceState(state.get))
        )
      case msg @ FireTransition(id, input, correlationId) =>
        fireTransitionById[S](id, input).run(instance).value match {
          case (updatedInstance, Right(job)) =>
            runJobAsync[Task, S, Any](job, executor).map {
              case ev: TransitionFiredEvent =>
                (
                  Command.persist(ev),
                  newState =>
                    zio.stream.Stream(
                      TransitionFired[S](ev.transitionId, ev.consumed, ev.produced, newState.map(instanceState).get)
                    )
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
                    (
                      Command.persist(e),
                      _ =>
                        zio.stream.Stream(
                          TransitionFailed(transitionId, consume, input, reason, strategy): TransitionResponse
                        )
                    )
                }
            }
          case (_, Left(reason)) =>
            UIO((Command.ignore, _ => zio.stream.Stream(TransitionNotEnabled(id, reason))))
        }
      case IdleStop(n) if n == instance.sequenceNr && instance.activeJobs.isEmpty =>
        context.self.flatMap((ar: ActorRef[Task]) => ar.stop).as((Command.ignore, _ => ()))

      case _: GetState[S] => UIO((Command.ignore, _ => instanceState(instance)))

    }
  }
  override def sourceEvent(state: Option[Instance[S]], event: Event): State = {
    event match {
      case init: InitializedEvent =>
        Option(applyEvent(Instance.uninitialized[S](topology))(init))
      case e =>
        state // TODO Has all the work really happened already? There was a call to step here before.
        state.map(i => applyEvent(i)(event))
    }
  }

  // TODO remove side effecting here
  def step(instance: Instance[S]): Task[Instance[S]] = {
    fireAllEnabledTransitions.run(instance).value match {
      case (updatedInstance, jobs) =>
        if (jobs.isEmpty && updatedInstance.activeJobs.isEmpty) {
          /* TODO: Needed?
          settings.idleTTL.foreach { ttl =>
            log.debug("Process has no running jobs, killing the actor in: {}", ttl)
            system.scheduler.scheduleOnce(ttl, context.self, PoisonPill)
          }
           */
        }

        ZIO
          .foreach(jobs.toSeq)(job => runJobAsync(job, executor))
          .map(_.foldLeft(updatedInstance)(applyEvent(_)(_)))
    }
  }
  def applyEvent(i: Instance[S])(e: Event): Instance[S] = EventSourcing.applyEvent(e).runS(i).value
}
