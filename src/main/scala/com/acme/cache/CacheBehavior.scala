package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
//import com.acme.cache.CacheBehaviorManager.{CacheBehaviorManagerMessage, CacheBehaviorManagerResponse, ShardedBackendValueResponse}

trait CacheBehaviorMessage
final case class TestCacheBehavior(testString: String, cacheBehaviorManager:  ActorRef[CacheBehaviorMessage]) extends CacheBehaviorMessage
final case class TestCacheResponse(response: String) extends CacheBehaviorMessage
class CacheBehavior(context: ActorContext[CacheBehaviorMessage], entityId: String) extends AbstractBehavior[CacheBehaviorMessage]{

    def onMessage(msg: CacheBehaviorMessage): Behavior[CacheBehaviorMessage] = msg match {
        case TestCacheBehavior(testString,replyTo) ⇒
            context.log.info("The test worked with - {}", testString)
            replyTo ! TestCacheResponse(testString)
            Behaviors.same
    }
}

object CacheBehavior {
    def apply(entityId:String): Behavior[CacheBehaviorMessage]
        = Behaviors.setup(context ⇒ new CacheBehavior(context,entityId))
}