package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.CommunityApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.Outcome.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.Outcome.SUCCESS
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import java.net.HttpURLConnection

class SmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Nested
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
    fun `test fails after first step`() {
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
  inner class WhenResetSucceeds {
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
      )
    }

    @Test
    fun `test succeeds`() {
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
        .expectNext(TestResult("Test for offender A7742DY with booking 38479A has completed successfully", SUCCESS))
        .verifyComplete()
    }

    @Nested
    @DisplayName("and the trigger fails")
    inner class WhenTriggerFails {
      @BeforeEach
      internal fun setUp() {
        PrisonApiExtension.prisonApi.stubFor(
          WireMock.post(WireMock.anyUrl()).willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_NOT_FOUND)
          )
        )
      }

      @Test
      fun `test fails after second step`() {
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
    @DisplayName("and the trigger erxperiences a server error")
    inner class WhenTriggerFallsOver {
      @BeforeEach
      internal fun setUp() {
        PrisonApiExtension.prisonApi.stubFor(
          WireMock.post(WireMock.anyUrl()).willReturn(
            WireMock.aResponse()
              .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)
          )
        )
      }

      @Test
      fun `test fails after second step`() {
        val results: FluxExchangeResult<TestResult> = webTestClient.post()
          .uri("/smoke-test")
          .accept(TEXT_EVENT_STREAM)
          .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
          .exchange()
          .expectStatus().isOk
          .returnResult(TestResult::class.java)

        StepVerifier.create(results.responseBody)
          .expectNext(TestResult("Reset Community test data for X360040"))
          .expectNextMatches { testResult -> testResult.description.contains("Trigger for A7742DY failed due to 500 Internal Server Error") }
          .verifyComplete()
      }
    }
  }
}
