package uk.gov.justice.digital.hmpps.dpssmoketest.service.ptpu

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL

@Service
class PrisonService(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient
) {

  fun getTestInputs(nomsNumber: String, crn: String): Mono<PtpuTestInputs> {

    data class OffenderDetails(val bookingNo: String, val agencyId: String)

    fun failToGetTestInputs(exception: Throwable): Mono<out PtpuTestInputs> =
      Mono.just(
        PtpuTestInputs(
          crn = crn,
          nomsNumber = nomsNumber,
          bookingNumber = "NOT FOUND",
          prisonCode = "NOT FOUND",
          testStatus = TestStatus("Unable to gather the test inputs due to exception ${exception.message}", FAIL)
        )
      )

    return webClient.get()
      .uri("/api/smoketest/offenders/{nomsNumber}/imprisonment-status", nomsNumber)
      .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
      .retrieve()
      .bodyToMono(OffenderDetails::class.java)
      .map {
        PtpuTestInputs(
          crn = crn,
          nomsNumber = nomsNumber,
          bookingNumber = it.bookingNo,
          prisonCode = it.agencyId,
          testStatus = TestStatus("Retrieved test inputs: $it")
        )
      }
      .onErrorResume(::failToGetTestInputs)
  }

  fun triggerTest(nomsNumber: String): Mono<TestStatus> {

    fun failOnNotFound(): Mono<out TestStatus> =
      Mono.just(TestStatus("Trigger test failed. The offender $nomsNumber can not be found", FAIL))

    fun failOnError(exception: Throwable): Mono<out TestStatus> =
      Mono.just(TestStatus("Trigger for $nomsNumber failed due to ${exception.message}", FAIL))

    return webClient.post()
      .uri("/api/smoketest/offenders/{nomsNumber}/imprisonment-status", nomsNumber)
      .contentType(MediaType.APPLICATION_JSON)
      .retrieve()
      .toBodilessEntity()
      .map { TestStatus("Triggered test for $nomsNumber") }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { failOnNotFound() }
      .onErrorResume(::failOnError)
  }
}
