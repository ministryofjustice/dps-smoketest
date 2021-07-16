package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.CommunityApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.ProbationOffenderSearchExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.SUCCESS
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PsiSmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Nested
  @DisplayName("Psi API tests")
  inner class ApiTests {
    @BeforeEach
    internal fun setUp() {
      stubCheckOffenderExists()
      stubChangeOffenderName()
      stubTestWillCompleteSuccessfully()
    }

    @Test
    fun `requires valid authentication token`() {
      webTestClient.post()
        .uri("/smoke-test/probation-search-indexer/PSI_T3")
        .accept(TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.post()
        .uri("/smoke-test/probation-search-indexer/PSI_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `requires valid test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/probation-search-indexer/NOT_A_TEST_PROFILE")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestStatus::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Unknown test profile NOT_A_TEST_PROFILE", FAIL))
        .verifyComplete()
    }

    @Test
    fun `succeeds with correct access and test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/probation-search-indexer/PSI_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(String::class.java)

      StepVerifier.create(results.responseBody).expectNextCount(6)
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When offender initial check fails")
  inner class WhenOffenderInitialCheckFails {
    @BeforeEach
    internal fun setUp() {
      stubCheckOffenderExists(HTTP_NOT_FOUND)
    }

    @Test
    fun `fails after first step`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Offender we expected to exist X379864 was not found. Check the offender has not be deleted in Delius", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When unable to change name")
  inner class WhenChangingNameFails {
    @BeforeEach
    internal fun setUp() {
      stubCheckOffenderExists()
      stubChangeOffenderName(HTTP_INTERNAL_ERROR)
    }

    @Test
    fun `fails after third step`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Offender X379864 exists and is good to go") }
        .expectNextMatches { testResult -> testResult.description.contains("Will update name to") }
        .expectNextMatches { testResult -> testResult.description.contains("Update offender details test failed for X379864 failed due t") }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test never completes")
  inner class TestNeverCompletes {
    @BeforeEach
    internal fun setUp() {
      stubCheckOffenderExists()
      stubChangeOffenderName()
      stubTestNeverCompletes()
    }

    @Test
    fun `fails on result not found`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Offender X379864 exists and is good to go") }
        .expectNextMatches { testResult -> testResult.description.contains("Will update name to") }
        .expectNextMatches { testResult -> testResult.description.contains("Offender details set for X379864") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult ->
          testResult.description.contains("Offender X379864 was never found") &&
            testResult.progress == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When final search does not match offender")
  inner class TestNonMatchingOffender {
    @BeforeEach
    internal fun setUp() {
      stubCheckOffenderExists()
      stubChangeOffenderName()
      stubTestCompletesWithNonMatch()
    }

    @Test
    fun `fails on result not found`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Offender X379864 exists and is good to go") }
        .expectNextMatches { testResult -> testResult.description.contains("Will update name to") }
        .expectNextMatches { testResult -> testResult.description.contains("Offender details set for X379864") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult ->
          testResult.description.contains("Offender X379864 found with name Meaty Bones") &&
            testResult.progress == COMPLETE
        }
        .expectNextMatches { testResult ->
          testResult.description.contains("The offender X379864 was not found with name") &&
            testResult.progress == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test succeeds")
  inner class TestSucceeds {
    @BeforeEach
    internal fun setUp() {
      stubCheckOffenderExists()
      stubChangeOffenderName()
      stubTestWillCompleteSuccessfully()
    }

    @Test
    fun `correct events are returned`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Offender X379864 exists and is good to go") }
        .expectNextMatches { testResult -> testResult.description.contains("Will update name to") }
        .expectNextMatches { testResult -> testResult.description.contains("Offender details set for X379864") }
        .expectNextMatches { testResult -> testResult.description.contains("Still waiting for offender X379864 to be found") }
        .expectNextMatches { testResult ->
          testResult.description.contains("Offender X379864 found") &&
            testResult.progress == COMPLETE
        }
        .expectNextMatches { testResult ->
          testResult.description.contains("Test for offender X379864 finished successfully") &&
            testResult.progress == SUCCESS
        }
        .verifyComplete()

      // and data reverted back to PSI Smoketest
      CommunityApiExtension.communityApi.verify(
        postRequestedFor(urlEqualTo("/secure/smoketest/offenders/crn/X379864/details")).withRequestBody(
          equalToJson(
            """{
                "firstName" : "PSI",
                "surname": "Smoketest"
            }""".trimMargin()
          )
        )
      )
      assertThat(getLastRequest().request.bodyAsString).contains("PSI").contains("Smoketest")
    }
  }

  private fun getLastRequest() = CommunityApiExtension.communityApi.serveEvents.requests.first()

  private fun postStartTest(): FluxExchangeResult<TestStatus> =
    webTestClient.post()
      .uri("/smoke-test/probation-search-indexer/PSI_T3")
      .accept(TEXT_EVENT_STREAM)
      .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
      .exchange()
      .expectStatus().isOk
      .returnResult(TestStatus::class.java)
}

private fun stubCheckOffenderExists(status: Int = HTTP_OK) =
  CommunityApiExtension.communityApi.stubFor(
    WireMock.get(WireMock.anyUrl()).willReturn(
      WireMock.aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(status)
        .withBody(
          """
            { "firstName": "Jane", "surname": "Smith" }
          """.trimIndent()
        )
    )
  )

private fun stubChangeOffenderName(status: Int = HTTP_OK) =
  CommunityApiExtension.communityApi.stubFor(
    WireMock.post(WireMock.anyUrl()).willReturn(
      WireMock.aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(status)
    )
  )

private fun stubTestNeverCompletes() {
  ProbationOffenderSearchExtension.probationOffenderSearch.stubFor(
    WireMock.post(WireMock.anyUrl()).willReturn(
      WireMock.aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(HTTP_OK)
        .withBody(
          """
            []
          """.trimIndent()
        )
    )
  )
}

private fun stubTestWillCompleteSuccessfully() {
  ProbationOffenderSearchExtension.probationOffenderSearch.stubFor(
    WireMock.post(WireMock.anyUrl())
      .inScenario("My Scenario")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        WireMock.aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withStatus(HTTP_OK)
          .withBody(
            """
            []
            """.trimIndent()
          )
      )
      .willSetStateTo("Found")
  )
  ProbationOffenderSearchExtension.probationOffenderSearch.stubFor(
    WireMock.post(WireMock.anyUrl())
      .inScenario("My Scenario")
      .whenScenarioStateIs("Found")
      .willReturn(
        WireMock.aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withStatus(HTTP_OK)
          .withTransformers("search-body")
      )
      .willSetStateTo("Found")
  )
}

private fun stubTestCompletesWithNonMatch() {
  ProbationOffenderSearchExtension.probationOffenderSearch.stubFor(
    WireMock.post(WireMock.anyUrl())
      .inScenario("My Scenario")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        WireMock.aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withStatus(HTTP_OK)
          .withBody(
            """
            []
            """.trimIndent()
          )
      )
      .willSetStateTo("Found")
  )
  ProbationOffenderSearchExtension.probationOffenderSearch.stubFor(
    WireMock.post(WireMock.anyUrl())
      .inScenario("My Scenario")
      .whenScenarioStateIs("Found")
      .willReturn(
        WireMock.aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withStatus(HTTP_OK)
          .withBody(
            """
            [
             {
                "otherIds": {
                  "crn": "X12345"
                },
                "firstName": "Meaty",
                "surname": "Bones"
             }
            ]
            """.trimIndent()
          )

      )
      .willSetStateTo("Found")
  )
}
