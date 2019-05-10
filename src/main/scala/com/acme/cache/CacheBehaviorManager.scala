package com.acme.cache

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
//import com.acme.cache.CacheBehaviorManager.{CacheBehaviorManagerMessage, GetShardedBackendValue}

class CacheBehaviorManager (context: ActorContext[Nothing]) extends AbstractBehavior[Nothing] {
//    (implicit backendClient: BackendClient[BackendRequest], typedActorSystem: ActorSystem[CacheBehaviorManagerMessage])
//    val sharding = ClusterSharding(typedActorSystem)
//    val TypeKey = EntityTypeKey[CacheBehaviorMessage](name = "cache-actor")

    override def onMessage(msg: Nothing): Behavior[Nothing] = {
//        case GetShardedBackendValue(key, request, replyTo) ⇒
//            val entity: Entity[CacheBehaviorMessage, ShardingEnvelope[CacheBehaviorMessage]] = Entity(TypeKey,createBehavior = ctx ⇒ CacheBehavior(context.self))
//            val cacheRef: EntityRef[CacheBehaviorMessage] = sharding.entityRefFor(TypeKey,key)
//            cacheRef ! TestCacheBehavior(msg)
            Behaviors.unhandled
    }

}

object CacheBehaviorManager {
//    trait CacheBehaviorManagerMessage
//    final case class GetShardedBackendValue(key: String, request: String, replyTo: ActorRef[CacheBehaviorManagerResponse]) extends CacheBehaviorManagerMessage
//    final case class ShardedBackendValueResponse(response: String, replyTo: ActorRef[CacheBehaviorManagerResponse]) extends CacheBehaviorManagerMessage
//
//    trait CacheBehaviorManagerResponse
//    final case class ShardedBackendValue(response: String) extends CacheBehaviorManagerResponse

    def apply() = Behaviors.setup[Nothing](context ⇒ new CacheBehaviorManager(context))


}
