package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL

@Service
class PrisonService(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
) {
  fun triggerPoeReleaseTest(nomsNumber: String): Mono<TestStatus> {
    fun failOnNotFound(): Mono<out TestStatus> =
      Mono.just(TestStatus("Trigger test failed. The offender $nomsNumber can not be found", FAIL))

    fun failOnError(exception: Throwable): Mono<out TestStatus> =
      Mono.just(TestStatus("Trigger for $nomsNumber failed due to ${exception.message}", FAIL))

    return webClient.put()
      .uri("/api/smoketest/offenders/{nomsNumber}/release", nomsNumber)
      .retrieve()
      .toBodilessEntity()
      .map { TestStatus("Triggered test for $nomsNumber") }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { failOnNotFound() }
      .onErrorResume(::failOnError)
  }

  fun triggerPoeRecallTest(nomsNumber: String): Mono<TestStatus> {
    fun failOnNotFound(): Mono<out TestStatus> =
      Mono.just(TestStatus("Trigger test failed. The offender $nomsNumber can not be found", FAIL))

    fun failOnError(exception: Throwable): Mono<out TestStatus> =
      Mono.just(TestStatus("Trigger for $nomsNumber failed due to ${exception.message}", FAIL))

    return webClient.put()
      .uri("/api/smoketest/offenders/{nomsNumber}/recall", nomsNumber)
      .retrieve()
      .toBodilessEntity()
      .map { TestStatus("Triggered test for $nomsNumber") }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { failOnNotFound() }
      .onErrorResume(::failOnError)
  }
}
