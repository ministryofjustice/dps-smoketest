package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.SUCCESS
import java.time.Duration

@Service
class CommunityService(
  @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
  @Value("\${test.resultPollMs}") private val testResultPollMs: Long,
  @Qualifier("communityApiWebClient") private val webClient: WebClient
) {

  fun resetTestData(crn: String): Mono<TestResult> {

    fun failOnNotFound(): Mono<out TestResult> =
      Mono.just(TestResult("Reset Community test failed. The offender $crn can not be found", FAIL))

    fun failOnError(exception: Throwable): Mono<out TestResult> =
      Mono.just(TestResult("Reset Community test data for $crn failed due to ${exception.message}", FAIL))

    return webClient.post()
      .uri("/secure/smoketest/offenders/crn/{crn}/custody/reset", crn)
      .contentType(MediaType.APPLICATION_JSON)
      .retrieve()
      .toBodilessEntity()
      .map { TestResult("Reset Community test data for $crn") }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { failOnNotFound() }
      .onErrorResume(::failOnError)
  }

  fun waitForTestToComplete(nomsNumber: String, bookingNumber: String): Flux<TestResult> =
    Flux.interval(Duration.ofMillis(testResultPollMs))
      .take(Duration.ofSeconds(testMaxLengthSeconds))
      .flatMap { checkTestComplete(nomsNumber, bookingNumber) }
      .takeUntil(TestResult::testComplete)

  fun checkTestComplete(nomsNumber: String, bookNumber: String): Mono<TestResult> {

    fun testIncompleteOnNotFound(): Mono<out TestResult> =
      Mono.just(TestResult("Still waiting for offender $nomsNumber with booking $bookNumber to be updated"))

    fun failOnError(exception: Throwable): Mono<out TestResult> =
      Mono.just(TestResult("Check test results for $nomsNumber failed due to ${exception.message}", FAIL))

    return webClient.get()
      .uri("/secure/offenders/nomsNumber/{nomsNumber}/custody/bookingNumber/{bookingNumber}", nomsNumber, bookNumber)
      .retrieve()
      .toBodilessEntity()
      .map { TestResult("Test for offender $nomsNumber with booking $bookNumber has completed", COMPLETE) }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { testIncompleteOnNotFound() }
      .onErrorResume(::failOnError)
  }

  fun assertTestResult(nomsNumber: String, bookingNumber: String, prisonCode: String): Mono<TestResult> {

    fun failOnError(exception: Throwable): Mono<out TestResult> =
      Mono.just(TestResult("Check test results for $nomsNumber failed due to ${exception.message}", FAIL))

    return getCustodyDetails(nomsNumber, bookingNumber).map {
      it.takeIf { it.matches(prisonCode) }
        ?.let { TestResult("Test for offender $nomsNumber with booking $bookingNumber finished with result", SUCCESS) }
        ?: TestResult(
          "Test for offender $nomsNumber with booking $bookingNumber failed with custodyDetails=$this",
          FAIL
        )
    }
      .onErrorResume(::failOnError)
  }

  private data class CustodyDetails(val bookingNumber: String, val institution: Institution, val status: Status)
  private data class Institution(val nomsPrisonInstitutionCode: String)
  private data class Status(val code: String)

  private fun getCustodyDetails(nomsNumber: String, bookNumber: String): Mono<CustodyDetails> =
    webClient.get()
      .uri("/secure/offenders/nomsNumber/{nomsNumber}/custody/bookingNumber/{bookingNumber}", nomsNumber, bookNumber)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(CustodyDetails::class.java)

  private fun CustodyDetails.matches(prisonCode: String) =
    this.institution.nomsPrisonInstitutionCode == prisonCode &&
      this.status.code == "D"
}
