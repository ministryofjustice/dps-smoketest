package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import java.time.Duration

@Service
class QueueService(
  @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
  @Value("\${test.resultPollMs}") private val testResultPollMs: Long
) {

  fun waitForEventToBeProduced(eventType: String, nomsNumber: String, finalStatus: TestProgress): Flux<TestStatus> =
    Flux.interval(Duration.ofMillis(testResultPollMs))
      .take(Duration.ofSeconds(testMaxLengthSeconds))
      .flatMap { checkEventProduced(eventType, nomsNumber, finalStatus) }
      .takeUntil(TestStatus::testComplete)

  fun checkEventProduced(eventType: String, nomsNumber: String, finalStatus: TestProgress): Mono<TestStatus> {

    fun failOnError(exception: Throwable): Mono<out TestStatus> =
      Mono.just(
        TestStatus("Check $eventType event produced $nomsNumber failed due to ${exception.message}", FAIL)
      )

    return Mono.just(checkForEvent(eventType, nomsNumber))
      .map {
        if (it)
          TestStatus("Test for offender $nomsNumber $eventType event finished successfully", finalStatus)
        else
          TestStatus("Still waiting for offender $nomsNumber $eventType event")
      }
      .onErrorResume(::failOnError)
  }

  fun checkForEvent(eventTypeRequired: String, nomsNumber: String): Boolean {
    return true
  }

  fun purgeQueue() {
    return
  }
}
