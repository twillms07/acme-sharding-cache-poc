package com.acme.cache

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.acme.cache.CacheActor.CacheActorMessage
import com.acme.cache.CacheActorManager._

import scala.collection.concurrent.TrieMap

class CacheActorManager(context: ActorContext[CacheActorManagerMessage]) extends AbstractBehavior[CacheActorManagerMessage] {

    val cacheMap: TrieMap[String, ActorRef[CacheActorMessage]] = TrieMap.empty

    def onMessage(msg: CacheActorManagerMessage): Behavior[CacheActorManagerMessage] = msg match  {

        case CacheActorRegisterNotification(key, actorRef) ⇒
            context.log.info("Cache Actor added - {}",key)
            cacheMap += (key → actorRef)
            Behaviors.same

        case CacheActorTimeoutNotification(key) ⇒
            cacheMap -= key
            context.log.info("Cache Actor removed - {}",key)
            Behaviors.same

        case GetCacheActors(replyTo) ⇒
            replyTo ! InCache(cacheMap.keys.toList)
            Behaviors.same
    }
}

object CacheActorManager {

    trait CacheActorManagerMessage
    final case class CacheActorRegisterNotification(key: String, actorRef: ActorRef[CacheActorMessage]) extends CacheActorManagerMessage
    final case class CacheActorTimeoutNotification(key: String) extends CacheActorManagerMessage
    final case class GetCacheActors(replyTo: ActorRef[CacheActorManagerResponse]) extends CacheActorManagerMessage

    trait CacheActorManagerResponse
    final case class InCache(cache: List[String] = List.empty) extends CacheActorManagerResponse

    def apply():Behavior[CacheActorManagerMessage] =
        Behaviors.setup[CacheActorManagerMessage](context ⇒ new CacheActorManager(context))
}
