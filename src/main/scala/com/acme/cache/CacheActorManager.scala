package com.acme.cache

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

class CacheActorManager(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing] {
    def onMessage(msg: Nothing): Behavior[Nothing] = {
        Behaviors.unhandled
    }
}

object CacheActorManager {
    def apply():Behavior[Nothing] =
        Behaviors.setup[Nothing](context ⇒ new CacheActorManager(context))
}
