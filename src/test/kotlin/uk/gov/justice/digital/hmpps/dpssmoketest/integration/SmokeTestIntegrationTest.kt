package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
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
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.SUCCESS
import java.net.HttpURLConnection

class SmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Nested
  @DisplayName("Authentication")
  inner class Authentication {
    @Test
    fun `requires valid authentication token`() {
      webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `succeeds with correct access`() {
      webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  @DisplayName("When reset test fails")
  inner class WhenResetFails {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
        )
      )
    }

    @Test
    fun `fails after first step`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestResult("Reset Community test failed. The offender X360040 can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When reset test errors")
  inner class WhenResetErrors {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)
        )
      )
    }

    @Test
    fun `fails after first step`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult ->
          testResult.description.contains("Reset Community test data for X360040 failed due to 500 Internal Server Error") &&
            testResult.testStatus == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When trigger test fails")
  inner class WhenTriggerFails {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      PrisonApiExtension.prisonApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
        )
      )
    }

    @Test
    fun `fails after second step`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestResult("Reset Community test data for X360040"))
        .expectNext(TestResult("Trigger test failed. The offender A7742DY can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When trigger test errors")
  inner class WhenTriggerErrors {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      PrisonApiExtension.prisonApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)
        )
      )
    }

    @Test
    fun `fails after second step`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestResult("Reset Community test data for X360040"))
        .expectNextMatches { testResult ->
          testResult.description.contains("Trigger for A7742DY failed due to 500 Internal Server Error") &&
            testResult.testStatus == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When check test results fails")
  inner class ResultsNotFound {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      PrisonApiExtension.prisonApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs(STARTED)
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
          )
          .willSetStateTo("Found")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Found")
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_OK)
          )
          .willSetStateTo("Return results")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Return results")
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
          )
      )
    }

    @Test
    fun `fails on result`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestResult("Reset Community test data for X360040"))
        .expectNext(TestResult("Triggered test for A7742DY"))
        .expectNext(TestResult("Still waiting for offender A7742DY with booking 38479A to be updated"))
        .expectNext(TestResult("Test for offender A7742DY with booking 38479A has completed", COMPLETE))
        .expectNextMatches { testResult ->
          testResult.description.contains("Check test results for A7742DY failed due to 404 Not Found") &&
            testResult.testStatus == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When check test results errors")
  inner class GettingResultsErrors {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      PrisonApiExtension.prisonApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs(STARTED)
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
          )
          .willSetStateTo("Found")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Found")
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_OK)
          )
          .willSetStateTo("Return results")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Return results")
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)
          )
      )
    }

    @Test
    fun `fails on result`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestResult("Reset Community test data for X360040"))
        .expectNext(TestResult("Triggered test for A7742DY"))
        .expectNext(TestResult("Still waiting for offender A7742DY with booking 38479A to be updated"))
        .expectNext(TestResult("Test for offender A7742DY with booking 38479A has completed", COMPLETE))
        .expectNextMatches { testResult ->
          testResult.description.contains("Check test results for A7742DY failed due to 500 Internal Server Error") &&
            testResult.testStatus == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test result data incorrect")
  inner class TestResultDataBad {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      PrisonApiExtension.prisonApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs(STARTED)
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
          )
          .willSetStateTo("Found")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Found")
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_OK)
          )
          .willSetStateTo("Return results")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Return results")
          .willReturn(
            WireMock.aResponse()
              .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpURLConnection.HTTP_OK)
              .withBody(
                """
                  { "bookingNumber": "38479A",
                    "institution": { "nomsPrisonInstitutionCode": "BWI" },
                    "status": { "code": "D" }
                  }
                """.trimIndent()
              )
          )
      )
    }

    @Test
    fun `fails due with incorrct custody details`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestResult("Reset Community test data for X360040"))
        .expectNext(TestResult("Triggered test for A7742DY"))
        .expectNext(TestResult("Still waiting for offender A7742DY with booking 38479A to be updated"))
        .expectNext(TestResult("Test for offender A7742DY with booking 38479A has completed", COMPLETE))
        .expectNextMatches { testResult ->
          testResult.description.contains("Test for offender A7742DY with booking 38479A failed with custodyDetails") &&
            testResult.testStatus == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test succeeds")
  inner class TestSucceeds {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      PrisonApiExtension.prisonApi.stubFor(
        WireMock.post(WireMock.anyUrl()).willReturn(
          WireMock.aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
        )
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs(STARTED)
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
          )
          .willSetStateTo("Found")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Found")
          .willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_OK)
          )
          .willSetStateTo("Return results")
      )
      CommunityApiExtension.communityApi.stubFor(
        WireMock.get(WireMock.anyUrl())
          .inScenario("My Scenario")
          .whenScenarioStateIs("Return results")
          .willReturn(
            WireMock.aResponse()
              .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .withStatus(HttpURLConnection.HTTP_OK)
              .withBody(
                """
                  { "bookingNumber": "38479A",
                    "institution": { "nomsPrisonInstitutionCode": "MDI" },
                    "status": { "code": "D" }
                  }
                """.trimIndent()
              )
          )
      )
    }

    @Test
    fun `correct events are returned`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
        .uri("/smoke-test")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestResult("Reset Community test data for X360040"))
        .expectNext(TestResult("Triggered test for A7742DY"))
        .expectNext(TestResult("Still waiting for offender A7742DY with booking 38479A to be updated"))
        .expectNext(TestResult("Test for offender A7742DY with booking 38479A has completed", COMPLETE))
        .expectNext(TestResult("Test for offender A7742DY with booking 38479A finished with result", SUCCESS))
        .verifyComplete()
    }
  }
}
