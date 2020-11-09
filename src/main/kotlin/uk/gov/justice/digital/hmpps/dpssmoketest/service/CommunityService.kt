package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import java.time.Duration

data class TestStatus(val testComplete: Boolean, val testResult: TestResult)

@Service
class CommunityService(
    @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
    @Value("\${test.resultPollMs}") private val testResultPollMs: Long,
    @Qualifier("communityApiWebClient") private val webClient: WebClient
) {

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

  /*
   * The number of times to poll before completing a SUCCEED or FAIL test
   * e.g. in SmokeTestIntegrationTest this is (2*10*1000)/(3*1000)=6 polls (taking 6 seconds)
   * e.g. in a Circle build this is (2*600*1000)/(3*10000)=40 polls (taking 400 seconds)
   */
  val maxTestPollCount = (2 * testMaxLengthSeconds * 1000) / (3 * testResultPollMs)

  fun resetTestData(crn: String): TestResult {
    fun failIfNotFound(exception: Throwable): Mono<out TestResult> =
        if (exception is WebClientResponseException.NotFound) Mono.just(TestResult("Reset Community test failed. The offender $crn can not be found", false)) else Mono.error(exception)

    fun failOnError(exception: Throwable): Mono<out TestResult> =
        Mono.just(TestResult("Reset Community test data for $crn failed due to ${exception.message}", false))


    return webClient.post()
        .uri("/secure/smoketest/offenders/crn/{crn}/custody/reset", crn)
        .retrieve()
        .toBodilessEntity()
        .map { TestResult("Reset Community test data for $crn") }
        .onErrorResume(::failIfNotFound)
        .onErrorResume(::failOnError)
        .block() ?: TestResult("Reset Community test data failed", false)
  }

  fun checkTestResults(nomsNumber: String, bookNumber: String): Flux<TestResult> {
    return Flux.interval(Duration.ofMillis(testResultPollMs))
        .take(Duration.ofSeconds(testMaxLengthSeconds))
        .flatMap {
          checkTestResult(nomsNumber, bookNumber)
        }
        .takeWhile {
          it.testComplete.not()
        }
        .map {
          it.testResult
        }
  }

  fun checkTestResult(nomsNumber: String, bookNumber: String): Mono<TestStatus> {
    fun notFoundYetOnNotFound(exception: Throwable): Mono<out TestStatus> =
        if (exception is WebClientResponseException.NotFound) Mono.just(TestStatus(false, TestResult("Still waiting for offender $nomsNumber with booking $bookNumber to be updated"))) else Mono.error(exception)

    return webClient.get()
        .uri("/secure/offenders/nomsNumber/{nomsNumber}/custody/bookingNumber/{bookingNumber}", nomsNumber, bookNumber)
        .retrieve()
        .toBodilessEntity()
        .map { TestStatus(true, TestResult("Test for offender $nomsNumber with booking $bookNumber has completed successfully", true)) }
        .onErrorResume(::notFoundYetOnNotFound)
  }

}