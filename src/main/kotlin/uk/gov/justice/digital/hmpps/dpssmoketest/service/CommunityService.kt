package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.Outcome.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.Outcome.SUCCESS
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import java.time.Duration

data class TestStatus(val testComplete: Boolean, val testResult: TestResult)

@Service
class CommunityService(
  @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
  @Value("\${test.resultPollMs}") private val testResultPollMs: Long,
  @Qualifier("communityApiWebClient") private val webClient: WebClient
) {

  fun resetTestData(crn: String): Mono<TestResult> {

    fun failIfNotFound(exception: Throwable): Mono<out TestResult> =
      if (exception is WebClientResponseException.NotFound)
        Mono.just(TestResult("Reset Community test failed. The offender $crn can not be found", FAIL))
      else
        Mono.error(exception)

    fun failOnError(exception: Throwable): Mono<out TestResult> =
      Mono.just(TestResult("Reset Community test data for $crn failed due to ${exception.message}", FAIL))

    return webClient.post()
      .uri("/secure/smoketest/offenders/crn/{crn}/custody/reset", crn)
      .contentType(MediaType.APPLICATION_JSON)
      .retrieve()
      .toBodilessEntity()
      .map { TestResult("Reset Community test data for $crn") }
      .onErrorResume(::failIfNotFound)
      .onErrorResume(::failOnError)
  }

  fun checkTestResults(nomsNumber: String, bookNumber: String): Flux<TestResult> {
    return Flux.interval(Duration.ofMillis(testResultPollMs))
      .take(Duration.ofSeconds(testMaxLengthSeconds))
      .flatMap { checkTestResult(nomsNumber, bookNumber) }
      .takeWhile { it.testComplete.not() }
      .map { it.testResult }
  }

  fun checkTestResult(nomsNumber: String, bookNumber: String): Mono<TestStatus> {
    fun notFoundYetOnNotFound(exception: Throwable): Mono<out TestStatus> =
      if (exception is WebClientResponseException.NotFound)
        Mono.just(TestStatus(false, TestResult("Still waiting for offender $nomsNumber with booking $bookNumber to be updated")))
      else
        Mono.error(exception)

    return webClient.get()
      .uri("/secure/offenders/nomsNumber/{nomsNumber}/custody/bookingNumber/{bookingNumber}", nomsNumber, bookNumber)
      .retrieve()
      .toBodilessEntity()
      .map { TestStatus(true, TestResult("Test for offender $nomsNumber with booking $bookNumber has completed successfully", SUCCESS)) }
      .onErrorResume(::notFoundYetOnNotFound)
  }
}
