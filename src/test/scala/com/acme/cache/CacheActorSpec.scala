package com.acme.cache

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit, TestInbox}
import akka.actor.typed.ActorRef
import akka.util.Timeout
import com.acme.cache.CacheActor._
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class CacheActorSpec extends ScalaTestWithActorTestKit with WordSpecLike with ScalaFutures{
    import TestBackendClient.TestBackendClientImpl
    override implicit val patienceConfig = PatienceConfig(timeout = 20 seconds)
    override implicit val timeout = Timeout(patienceConfig.timeout)

    "Cache actor " must {
        "Start out in init behavior and when receiving a CacheRequest request from the backend" in {
            val testHttpServer = createTestProbe[CacheActorResponse]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey"))
            testCache ! CacheActorRequest("testRequest1",testHttpServer.ref)
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
        }

        "Stash all additional requests while waiting for the backend response " in {
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey"))
            val testHttpServer = createTestProbe[CacheActorResponse]()
            testCache ! CacheActorRequest("testRequest1", testHttpServer.ref)
            testCache ! CacheActorRequest("testRequest2", testHttpServer.ref)
            testCache ! CacheActorRequest("testRequest3", testHttpServer.ref)
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
            testHttpServer.expectMessage(CacheActorSuccess("TestBackendResponse-testRequest1"))
        }

        "Receive a cacheTimeout message when the cache needs to be renewed " in {
//            val testKit1 =  BehaviorTestKit(CacheActor("TestKey"))
            val inbox = TestInbox[CacheActorMessage]()
            val testHttpServer = createTestProbe[CacheActorResponse]()
            val testCache: ActorRef[CacheActor.CacheActorMessage] = spawn(CacheActor("testKey"))
            testCache ! CacheActorRequest("testRequest1", testHttpServer.ref)
            inbox.expectMessage(CacheActorTimeout)
        }

    }
}
