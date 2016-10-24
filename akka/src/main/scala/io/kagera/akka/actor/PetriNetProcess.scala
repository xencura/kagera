package io.kagera.akka.actor

import akka.actor.{ ActorLogging, ActorRef, Props }
import akka.pattern.pipe
import akka.persistence.PersistentActor
import fs2.Strategy
import io.kagera.akka.actor.PetriNetEventSourcing._
import io.kagera.akka.actor.PetriNetExecution._
import io.kagera.akka.actor.PetriNetProcessProtocol._
import io.kagera.api.colored.ExceptionStrategy.RetryWithDelay
import io.kagera.api.colored._

import scala.concurrent.duration._
import scala.language.existentials

object PetriNetProcess {

  def props[S](process: ExecutablePetriNet[S]): Props = Props(new PetriNetProcess[S](process))
}

/**
 * This actor is responsible for maintaining the state of a single petri net instance.
 */
class PetriNetProcess[S](override val process: ExecutablePetriNet[S])
    extends PersistentActor
    with ActorLogging
    with PetriNetActorRecovery[S] {

  val processId = context.self.path.name

  override def persistenceId: String = s"process-$processId"

  import context.dispatcher

  override def receiveCommand = uninitialized

  def uninitialized: Receive = { case Initialize(marking, state) =>
    persistEvent(Instance.uninitialized(process), InitializedEvent(marking, state.asInstanceOf[S])) {
      (updatedState, e) =>
        executeAllEnabledTransitions(updatedState)
        sender() ! Initialized(marking, state)
    }
  }

  def running(instance: Instance[S]): Receive = {
    case GetState =>
      sender() ! instance.processState

    case e @ TransitionFiredEvent(jobId, transitionId, timeStarted, timeCompleted, consumed, produced, output) =>
      persistEvent(instance, e) { (updateInstance, e) =>
        log.debug(s"Transition fired ${transitionId}")
        executeAllEnabledTransitions(updateInstance)
        sender() ! TransitionFired[S](
          transitionId,
          e.consumed,
          e.produced,
          updateInstance.marking,
          updateInstance.state
        )
      }

    case e @ TransitionFailedEvent(
          jobId,
          transitionId,
          timeStarted,
          timeFailed,
          consume,
          input,
          reason,
          strategy @ RetryWithDelay(delay)
        ) =>
      val updatedInstance = applyEvent(e)(instance)._1

      log.warning(s"Transition '${transitionId}' failed: {}", reason)

      log.info(s"Scheduling a retry of transition ${transitionId} in $delay milliseconds")
      val originalSender = sender()
      system.scheduler.scheduleOnce(delay milliseconds) { executeJob(updatedInstance.jobs(jobId), originalSender) }

      sender() ! TransitionFailed(transitionId, consume, input, reason, strategy)
      context become running(updatedInstance)

    case e @ TransitionFailedEvent(jobId, transitionId, timeStarted, timeFailed, consume, input, reason, strategy) =>
      val updatedInstance = applyEvent(e)(instance)._1

      log.warning(s"Transition '${transitionId}' failed: {}", reason)
      sender() ! TransitionFailed(transitionId, consume, input, reason, strategy)

      context become running(updatedInstance)

    case FireTransition(id, input, correlationId) =>
      log.debug(s"Received message to fire transition $id with input: $input")

      process.findTransitionById(id) match {
        case Some(transition) =>
          fireTransition(transition, input)(instance) match {
            case (_, Right(notEnabledReason)) =>
              sender() ! TransitionNotEnabled(transition.id, notEnabledReason)
            case (newState, Left(job)) =>
              executeJob(job, sender())
              context become running(newState)
          }
        case None =>
          val msg = s"No transition exists with id: $id"
          sender() ! TransitionNotEnabled(id, msg)
          log.warning(msg)
      }
  }

  def executeAllEnabledTransitions(instance: Instance[S]) = {
    val (updatedInstance, jobs) = fireAllEnabledTransitions(instance)
    jobs.foreach(job => executeJob(job, sender()))
    context become running(updatedInstance)
  }

  // TODO: best to use another thread pool
  implicit val s: Strategy = Strategy.fromExecutionContext(context.dispatcher)

  def executeJob[E](job: Job[S, E], originalSender: ActorRef) =
    runJob(job).unsafeRunAsyncFuture().pipeTo(context.self)(originalSender)

  override def onRecoveryCompleted(instance: Instance[S]) = executeAllEnabledTransitions(instance)
}
