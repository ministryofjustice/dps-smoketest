package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.INCOMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.SUCCESS
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PoeSmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Nested
  @DisplayName("Poe API tests")
  inner class ApiTests {

    @BeforeEach
    internal fun setUp() {
      stubTriggerTest()
    }

    @Test
    fun `requires valid authentication token`() {
      webTestClient.post()
        .uri("/smoke-test/prison-offender-events/POE_T3")
        .accept(TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.post()
        .uri("/smoke-test/prison-offender-events/POE_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `requires valid test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/prison-offender-events/NOT_A_TEST_PROFILE")
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
    @Disabled
    fun `succeeds with correct access and test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/prison-offender-events/POE_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  @DisplayName("When trigger test fails")
  inner class WhenTriggerFails {
    @BeforeEach
    internal fun setUp() {
      stubTriggerTest(HTTP_NOT_FOUND)
    }

    @Test
    fun `status reflects error`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Will release prisoner A7851DY", INCOMPLETE))
        .expectNext(TestStatus("Trigger test failed. The offender A7851DY can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test succeeds")
  inner class TestSucceeds {
    @BeforeEach
    internal fun setUp() {
      stubTriggerTest()
    }

    @Test
    fun `status reflects success`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Will release prisoner A7851DY", INCOMPLETE))
        .expectNext(TestStatus("Triggered test for A7851DY"))
        .expectNext(TestStatus("Test for offender A7851DY released event finished successfully", SUCCESS))
        .verifyComplete()
    }
  }

  private fun postStartTest(): FluxExchangeResult<TestStatus> =
    webTestClient.post()
      .uri("/smoke-test/prison-offender-events/POE_T3")
      .accept(TEXT_EVENT_STREAM)
      .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
      .exchange()
      .expectStatus().isOk
      .returnResult(TestStatus::class.java)

  private fun stubTriggerTest(status: Int = HTTP_OK) =
    PrisonApiExtension.prisonApi.stubFor(
      WireMock.put(WireMock.anyUrl()).willReturn(
        WireMock.aResponse()
          .withStatus(status)
      )
    )
}
