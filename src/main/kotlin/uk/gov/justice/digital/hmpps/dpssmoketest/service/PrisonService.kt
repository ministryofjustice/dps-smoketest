package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.Outcome.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult

@Service
class PrisonService(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient
) {

  fun triggerTest(nomsNumber: String): Mono<TestResult> {

    fun failIfNotFound(exception: Throwable): Mono<out TestResult> =
      if (exception is WebClientResponseException.NotFound)
        Mono.just(TestResult("Trigger test failed. The offender $nomsNumber can not be found", FAIL))
      else
        Mono.error(exception)

    fun failOnError(exception: Throwable): Mono<out TestResult> =
      Mono.just(TestResult("Trigger for $nomsNumber failed due to ${exception.message}", FAIL))

    return webClient.post()
      .uri("/api/smoketest/offenders/{nomsNumber}/imprisonment-status", nomsNumber)
      .contentType(MediaType.APPLICATION_JSON)
      .retrieve()
      .toBodilessEntity()
      .map { TestResult("Triggered test for $nomsNumber") }
      .onErrorResume(::failIfNotFound)
      .onErrorResume(::failOnError)
  }
}
