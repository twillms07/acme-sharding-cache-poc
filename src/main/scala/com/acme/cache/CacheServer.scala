package com.acme.cache

import akka.actor
import akka.actor.Scheduler
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.acme.cache.CacheActor.{CacheActorResponse, CacheActorMessage, CacheActorRequest}
import com.acme.cache.CacheServer.typedActorSystem

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

class CacheServer

object CacheServer extends App with CacheRoutes {
    import akka.actor.typed.scaladsl.adapter._
    import com.acme.cache.ReferenceBackend.ReferenceBackendImpl

    val typedActorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](CacheActorManager(), name = "CacheManager")
    val sharding: ClusterSharding = ClusterSharding(typedActorSystem)
    val TypeKey: EntityTypeKey[CacheActorMessage] = EntityTypeKey[CacheActorMessage](name = "cache-actor")
    val shardRegion: ActorRef[ShardingEnvelope[CacheActorMessage]] =
        sharding.init(Entity(typeKey = TypeKey, createBehavior = context ⇒ CacheActor(context.entityId)))

    implicit val actorSystem: actor.ActorSystem = typedActorSystem.toUntyped
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = 3 seconds
    implicit val scheduler: Scheduler = actorSystem.scheduler

    val logger: LoggingAdapter = Logging(actorSystem, classOf[CacheServer])
    val interface = "0.0.0.0"
    val port = 8080

    Http().bindAndHandle(routes, interface = interface, port = port)

    logger.debug("Cache System up")

    try {
        logger.info(">>> Press ENTER to exit <<<")
        StdIn.readLine()
    } catch {
        case NonFatal(e) => typedActorSystem.terminate()
    }finally {
        typedActorSystem.terminate()
    }

}

trait CacheRoutes extends JsonSupport {

    implicit val scheduler: Scheduler
    implicit val timeout: Timeout
    val typedActorSystem:ActorSystem[Nothing]

    val sharding: ClusterSharding
    val TypeKey: EntityTypeKey[CacheActorMessage]
    val logger: LoggingAdapter

    val getCache: Route = path(pm = "cache" / "key" / Segment / "request" / Segment) { (key,request) ⇒
        get {
            val cacheRef: EntityRef[CacheActorMessage] = sharding.entityRefFor(TypeKey,key)
            val resultMaybe = cacheRef.ask(ref ⇒ CacheActorRequest(request,ref) )
            onComplete(resultMaybe) {
                case Success(result) ⇒
                    complete(StatusCodes.OK, result)
                case Failure(exception) ⇒
                    complete(StatusCodes.InternalServerError, exception)
            }
        }
    }

    val routes: Route = getCache

}