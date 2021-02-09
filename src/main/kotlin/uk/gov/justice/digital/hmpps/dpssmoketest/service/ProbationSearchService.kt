package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource
import java.time.Duration

@Service
class ProbationSearchService(
  @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
  @Value("\${test.resultPollMs}") private val testResultPollMs: Long,
  @Qualifier("probationOffenderSearchWebClient") private val webClient: WebClient
) {
  fun waitForOffenderToBeFound(crn: String, firstName: String, surname: String): Flux<SmokeTestResource.TestStatus> =
    Flux.interval(Duration.ofMillis(testResultPollMs))
      .take(Duration.ofSeconds(testMaxLengthSeconds))
      .flatMap { checkSearchTestComplete(crn, firstName, surname) }
      .takeUntil(SmokeTestResource.TestStatus::testComplete)

  fun checkSearchTestComplete(crn: String, firstName: String, surname: String): Mono<SmokeTestResource.TestStatus> {

    fun failOnError(exception: Throwable): Mono<out SmokeTestResource.TestStatus> =
      Mono.just(
        SmokeTestResource.TestStatus(
          "Check test results for $crn failed due to ${exception.message}",
          SmokeTestResource.TestStatus.TestProgress.FAIL
        )
      )

    return searchForOffender(crn, firstName, surname)
      .map {
        if (it.isEmpty()) {
          SmokeTestResource.TestStatus("Still waiting for offender $crn to be found with name $firstName $surname")
        } else {
          SmokeTestResource.TestStatus(
            "Offender $crn found with name ${it[0].firstName} ${it[0].surname}",
            SmokeTestResource.TestStatus.TestProgress.COMPLETE
          )
        }
      }
      .onErrorResume(::failOnError)
  }

  fun assertTestResult(crn: String, firstName: String, surname: String): Mono<SmokeTestResource.TestStatus> {

    fun failOnError(exception: Throwable): Mono<out SmokeTestResource.TestStatus> =
      Mono.just(
        SmokeTestResource.TestStatus(
          "Check test results for $crn failed due to ${exception.message}",
          SmokeTestResource.TestStatus.TestProgress.FAIL
        )
      )

    return searchForOffender(crn, firstName, surname).map {
      if (it.size != 1) {
        SmokeTestResource.TestStatus("Offender $crn was never found with name $firstName $surname", SmokeTestResource.TestStatus.TestProgress.FAIL)
      } else {
        val offenderDetails = it[0]
        if (offenderDetails.firstName == firstName && offenderDetails.surname == surname && offenderDetails.otherIds.crn == crn) {
          SmokeTestResource.TestStatus(
            "Test for offender $crn finished successfully",
            SmokeTestResource.TestStatus.TestProgress.SUCCESS
          )
        } else {
          SmokeTestResource.TestStatus("The offender $crn was not found with name $firstName $surname", SmokeTestResource.TestStatus.TestProgress.FAIL)
        }
      }
    }.onErrorResume(::failOnError)
  }

  private fun searchForOffender(crn: String, firstName: String, surname: String): Mono<List<OffenderDetails>> {
    return webClient.post()
      .uri("/search")
      .contentType(MediaType.APPLICATION_JSON)
      .body(searchByCrnNameBody(crn, firstName, surname))
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<OffenderDetails>>() {})
  }

  private fun searchByCrnNameBody(
    crn: String,
    firstName: String,
    surname: String
  ) = BodyInserters.fromValue(
    """
              {
                "crn": "$crn",
                "firstName": "$firstName",
                "surname": "$surname"
              }
    """.trimIndent()
  )
}

private data class IDs(val crn: String)
private data class OffenderDetails(val firstName: String, val surname: String, val otherIds: IDs)
