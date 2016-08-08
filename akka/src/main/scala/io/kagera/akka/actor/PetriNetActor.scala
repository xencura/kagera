//package io.kagera.akka.actor
//
//import akka.actor._
//import io.kagera.akka.actor.PetriNetActor._
//import io.kagera.akka.actor.PetriNetDebugging.Step
//import io.kagera.api._
//import io.kagera.api.colored._
//
//object PetriNetActor {
//
//  def props[S](id: java.util.UUID, process: TransitionExecutor[S], initialMarking: ColoredMarking, initialState: S) =
//    Props(new PetriNetActor(id, process, initialMarking, initialState))
//
//  sealed trait Command
//
//  case object GetState extends Command
//  case class FireTransition[T, I](transition: Transition, data: Option[Any] = None)
//
//  sealed trait Event
//
//  case class TransitionFailed(t: Transition, reason: Exception) extends RuntimeException
//  case class TransitionFired[O](t: Transition, consumed: ColoredMarking, produced: ColoredMarking, out: O) extends Event
//
//  case object NoFireableTransitions extends IllegalStateException
//}
//
//class PetriNetActor[S](id: java.util.UUID, process: TransitionExecutor[S], initialMarking: ColoredMarking, initialState: S) extends Actor with ActorLogging {
//
//  def receive = active(initialMarking, initialState)
//
//  def active(marking: ColoredMarking, state: S): Receive = {
//    case GetState =>
//      sender() ! marking
//    case Step =>
//      TokenGame.stepRandom[Place[_], Transition, ColoredMarking](process, marking) match {
//        case None => sender() ! Status.Failure(NoFireableTransitions)
//        case Some((consume, t)) =>
//
//          val produce =
//            process.executeTransition(process)(consume, t, None, id).map(
//              produce => marking.consume(consume).produce(produce)
//            )
//      }
//
//    case FireTransition(t, data) =>
//  }
//}
