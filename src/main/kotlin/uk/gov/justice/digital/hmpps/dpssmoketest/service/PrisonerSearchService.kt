package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import java.time.Duration

@Service
class PrisonerSearchService(
  @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
  @Value("\${test.resultPollMs}") private val testResultPollMs: Long,
  @Qualifier("prisonerSearchWebClient") private val webClient: WebClient,
) {

  fun checkOffenderExists(nomsNumber: String): Mono<TestStatus> {
    fun failOnError(exception: Throwable): Mono<out TestStatus> = Mono.just(
      TestStatus(
        "Offender we expected to exist $nomsNumber failed due to  ${exception.message}",
        FAIL,
      ),
    )

    fun failOnNotFound(): Mono<out TestStatus> = Mono.just(
      TestStatus(
        "Offender we expected to exist $nomsNumber was not found. Check the offender has not be deleted in NOMIS",
        FAIL,
      ),
    )

    return webClient.get()
      .uri("/prisoner/{id}", nomsNumber)
      .retrieve()
      .bodyToMono(OffenderDetails::class.java)
      .map { TestStatus("Offender $nomsNumber exists and is good to go. Current name is ${it.firstName} ${it.lastName}") }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { failOnNotFound() }
      .onErrorResume(::failOnError)
  }

  fun waitForOffenderToBeFound(nomsNumber: String, firstName: String, lastName: String): Flux<TestStatus> = Flux.interval(Duration.ofMillis(testResultPollMs))
    .take(Duration.ofSeconds(testMaxLengthSeconds))
    .flatMap { checkSearchTestComplete(nomsNumber, firstName, lastName) }
    .takeUntil(TestStatus::testComplete)

  fun checkSearchTestComplete(nomsNumber: String, firstName: String, lastName: String): Mono<TestStatus> {
    fun failOnError(exception: Throwable): Mono<out TestStatus> = Mono.just(
      TestStatus("Check test results for $nomsNumber failed due to ${exception.message}", FAIL),
    )

    return searchForOffender(nomsNumber, firstName, lastName)
      .map {
        it.find { od -> nomsNumber == od.prisonerNumber && firstName == od.firstName && lastName == od.lastName }?.let {
          TestStatus(
            "Offender $nomsNumber found with name $firstName $lastName",
            COMPLETE,
          )
        } ?: TestStatus("Still waiting for offender $nomsNumber to be found with name $firstName $lastName. Found $it instead")
      }
      .onErrorResume(::failOnError)
  }

  fun assertTestResult(prisonerNumber: String, firstName: String, lastName: String): Mono<TestStatus> {
    fun failOnError(exception: Throwable): Mono<out TestStatus> = Mono.just(
      TestStatus(
        "Check test results for $prisonerNumber failed due to ${exception.message}",
        FAIL,
      ),
    )

    return searchForOffender(prisonerNumber, firstName, lastName).map {
      if (it.size != 1) {
        TestStatus("Offender $prisonerNumber was never found with name $firstName $lastName", FAIL)
      } else {
        val offenderDetails = it[0]
        if (offenderDetails.firstName == firstName && offenderDetails.lastName == lastName && offenderDetails.prisonerNumber == prisonerNumber) {
          TestStatus(
            "Test for offender $prisonerNumber finished successfully",
            TestStatus.TestProgress.SUCCESS,
          )
        } else {
          TestStatus("The offender $prisonerNumber was not found with name $firstName $lastName", FAIL)
        }
      }
    }.onErrorResume(::failOnError)
  }

  private fun searchForOffender(nomsNumber: String, firstName: String, lastName: String): Mono<List<OffenderDetails>> = webClient.post()
    .uri("/global-search")
    .contentType(MediaType.APPLICATION_JSON)
    .body(searchByNumberNameBody(nomsNumber, firstName, lastName))
    .retrieve()
    .bodyToMono(SearchResults::class.java).map { it.content }

  private fun searchByNumberNameBody(
    prisonerNumber: String,
    firstName: String,
    lastName: String,
  ) = BodyInserters.fromValue(
    """
        {
          "prisonerIdentifier": "$prisonerNumber",
          "firstName": "$firstName",
          "lastName": "$lastName"
        }
    """.trimIndent(),
  )
}

private data class SearchResults(val content: List<OffenderDetails>)

private data class OffenderDetails(val firstName: String, val lastName: String, val prisonerNumber: String)
