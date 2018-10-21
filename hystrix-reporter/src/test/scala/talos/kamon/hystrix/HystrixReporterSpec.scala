package talos.kamon.hystrix

import java.time.{Clock, Instant, ZoneOffset}

import akka.actor.{ActorRef, ActorSystem}
import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import akka.pattern.CircuitBreaker
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import kamon.{Kamon, MetricReporter}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import talos.events.TalosEvents.model.CircuitBreakerEvent
import talos.http.CircuitBreakerStatsActor.CircuitBreakerStats
import talos.kamon.StatsAggregator

import scala.concurrent.Await
import scala.concurrent.duration._

object HystrixReporterSpec {
  import talos.events.syntax._

  implicit val testClock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)

  def createCircuitBreaker(name: String = "testCircuitBreaker")
                          (implicit actorSystem: ActorSystem) =
    CircuitBreaker.withEventReporting(
      "testCircuitBreaker",
      actorSystem.scheduler,
      5,
      2 seconds,
      5 seconds
    )

  def fireSuccessful(times: Int, circuitBreaker: CircuitBreaker): Seq[Int] =
    for (i <- 1 to times) yield circuitBreaker.callWithSyncCircuitBreaker(() => i)
}

class HystrixReporterSpec
      extends TestKit(ActorSystem("HystrixReporterSpec"))
      with Matchers
      with WordSpecLike
      with BeforeAndAfterAll
      with ImplicitSender
{
  import HystrixReporterSpec._

  override def afterAll(): Unit = {
    system.terminate()
    Kamon.stopAllReporters()
    system.eventStream.unsubscribe(statsAggregator)
  }


  val statsAggregator: ActorRef = {
    import akka.actor.typed.scaladsl.adapter._
    implicit val timeout: Timeout = Timeout(2 seconds)
    val typedActor =
      Await.result(system.toTyped.systemActorOf[CircuitBreakerEvent](StatsAggregator.behavior(), "statsAggregator"), 2 seconds)
    typedActor.toUntyped
  }
  system.eventStream.subscribe(statsAggregator, classOf[CircuitBreakerEvent])

  "Hystrix reporter receiving kamon metric snapshots" can {

    val circuitBreaker = createCircuitBreaker()
    val statsGatherer: TestProbe = TestProbe()

    val hystrixReporter: MetricReporter = new HystrixReporter(statsGatherer.ref)
    Kamon.addReporter(hystrixReporter)

    "group successful metrics into one single snapshot event" in {
      val results: Seq[Int] = fireSuccessful(10, circuitBreaker)
      val statsMessage = statsGatherer.expectMsgType[CircuitBreakerStats]
    }

  }

}