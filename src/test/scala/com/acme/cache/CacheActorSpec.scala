package com.acme.cache

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import com.acme.cache.CacheActor._
import com.acme.cache.CacheActorManager.{CacheActorManagerMessage, CacheActorRegisterNotification, CacheActorTimeoutNotification}
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class CacheActorSpec extends ScalaTestWithActorTestKit with WordSpecLike with ScalaFutures{
    import com.acme.cache.TestBackendClient.TestBackendClientImpl

    "Cache actor " must {
        "Start out in init behavior and when receiving a CacheRequest request from the backend" in {
            val testHttpServer = createTestProbe[CacheActorResponse]()
            val testCacheManager = createTestProbe[CacheActorManagerMessage]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey", testCacheManager.ref))
            testCache ! CacheActorRequest("testRequest1",testHttpServer.ref)
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
        }

        "Stash all additional requests while waiting for the backend response " in {
            val testCacheManager = createTestProbe[CacheActorManagerMessage]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey", testCacheManager.ref))
            val testHttpServer = createTestProbe[CacheActorResponse]()
            testCache ! CacheActorRequest("testRequest1", testHttpServer.ref)
            testCache ! CacheActorRequest("testRequest2", testHttpServer.ref)
            testCache ! CacheActorRequest("testRequest3", testHttpServer.ref)
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
        }

        "Receive a cacheTimeout message when the cache needs to be renewed " in {
            val testCacheManager = createTestProbe[CacheActorManagerMessage]()
            val testHttpServer = createTestProbe[CacheActorResponse]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey", testCacheManager.ref),"test-cache-actor")
            testCache ! CacheActorRequest("testRequest1", testHttpServer.ref)
            testCacheManager.expectMessageType[CacheActorRegisterNotification]
            testCacheManager.expectMessage(20 seconds, CacheActorTimeoutNotification("testKey"))
        }

    }
}
