package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl._
import com.acme.cache.CacheActor._
import com.acme.cache.CacheActorManager.{CacheActorManagerMessage, CacheActorRegisterNotification, CacheActorTimeoutNotification}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CacheActor(context: ActorContext[CacheActorMessage], key: String, cacheManager: ActorRef[CacheActorManagerMessage])
                (implicit backendClient: BackendClient[BackendRequest]) extends AbstractBehavior[CacheActorMessage] with BackendClientService {

    private var cacheValue: String = ""
    val buffer: StashBuffer[CacheActorMessage] = StashBuffer[CacheActorMessage](capacity = 100)
    val timerKey = s"timerKey-$key"
    val timerDelay: FiniteDuration = 5 seconds


    def onMessage(msg: CacheActorMessage): Behavior[CacheActorMessage] = init(msg)

    def init(msg: CacheActorMessage): Behavior[CacheActorMessage] = msg match {
        case CacheActorRequest(request, replyTo) ⇒
            context.log.debug(template = "Received initial request {} will get backend response.", request)
            val backendResponseF: Future[String] = getBackendClientResponse(BackendRequest(request))
            context.pipeToSelf(backendResponseF){
                case Success(r) ⇒
                    BackendResponse(r, replyTo)
                case Failure(e) ⇒
                    BackendErrorResponse(e,replyTo)
            }
            Behaviors.withTimers(timer ⇒ waiting(msg,timer))
    }

    def waiting(msg:CacheActorMessage, timer: TimerScheduler[CacheActorMessage]):Behavior[CacheActorMessage] = Behaviors.receiveMessage{
        case CacheActorRequest(request,replyTo) ⇒
            context.log.debug(template = "In waiting received request - ",request)
            buffer.stash(msg)
            waiting(msg,timer)
        case BackendResponse(response,replyTo) ⇒
            context.log.debug(template = "Received backend response - ",response)
            timer.startSingleTimer(timerKey, CacheActorTimeout, timerDelay)
            cacheValue = response
            cacheManager ! CacheActorRegisterNotification(key, context.self)
            replyTo ! CacheActorSuccess(response)
            buffer.unstashAll(context, available(msg))
        case BackendErrorResponse(e,replyTo) ⇒
            context.log.error(template = "Received backend error - ",e.getMessage)
            replyTo ! CacheActorFailure(e)
            buffer.unstashAll(context, init(msg))
    }

    def available(msg: CacheActorMessage): Behavior[CacheActorMessage] = Behaviors.receiveMessage {
        case CacheActorRequest(request, replyTo) ⇒
            context.log.debug(template = "Received backend response - ",request)
            replyTo ! CacheActorSuccess(cacheValue)
            available(msg)

        case CacheActorTimeout ⇒
            cacheManager ! CacheActorTimeoutNotification(key)
            context.log.info( template = "Cache Actor is being stopped - {}", key)
            Behaviors.stopped
    }

    override def onSignal: PartialFunction[Signal, Behavior[CacheActorMessage]] = {
        case PostStop ⇒
            context.log.info(template = "Cache with key {} shutting down", key)
            Behaviors.same
    }
}

object CacheActor {

    trait CacheActorMessage
    final case class CacheActorRequest(request:String, replyTo: ActorRef[CacheActorResponse]) extends CacheActorMessage
    final case class BackendResponse(response: String, replyTo: ActorRef[CacheActorResponse]) extends CacheActorMessage
    final case class BackendErrorResponse(e:Throwable, replyTo: ActorRef[CacheActorResponse]) extends CacheActorMessage
    final case object CacheActorTimeout extends CacheActorMessage

    trait CacheActorResponse
    final case class CacheActorSuccess(response: String) extends CacheActorResponse
    final case class CacheActorFailure(e: Throwable) extends CacheActorResponse

    def apply(key: String, cacheManager:ActorRef[CacheActorManagerMessage])(implicit backendClient: BackendClient[BackendRequest]): Behavior[CacheActorMessage] =
        Behaviors.setup(context ⇒ new CacheActor(context, key, cacheManager))
}

