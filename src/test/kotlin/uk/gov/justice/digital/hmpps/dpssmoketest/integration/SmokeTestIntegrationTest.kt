package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.CommunityApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import uk.gov.justice.digital.hmpps.dpssmoketest.service.CommunityService
import java.net.HttpURLConnection

class SmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  private lateinit var communityService: CommunityService

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
  inner class WhenRestFails {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse()
          .withStatus(HttpURLConnection.HTTP_NOT_FOUND)))
    }

    @Test
    fun `test fails after first step`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
          .uri("/smoke-test?testMode=SUCCEED")
          .accept(TEXT_EVENT_STREAM)
          .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
          .exchange()
          .expectStatus().isOk
          .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
          .expectNext(TestResult("Reset Community test failed. The offender X360040 can not be found", false))
          .thenCancel()
          .verify()
    }
  }

  @Nested
  inner class WhenRestSucceeds {
    @BeforeEach
    internal fun setUp() {
      CommunityApiExtension.communityApi.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse()
          .withStatus(HttpURLConnection.HTTP_OK)))
    }
    @Test
    fun `test succeeds`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
          .uri("/smoke-test?testMode=SUCCEED")
          .accept(TEXT_EVENT_STREAM)
          .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
          .exchange()
          .expectStatus().isOk
          .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
          .expectNext(TestResult("Reset Community test data for X360040"), TestResult("Test triggered"))
          .expectNextSequence(inProgressResults(communityService.maxTestPollCount.toInt(), "SUCCEED"))
          .expectNext(TestResult("Test has completed successfully", true))
          .thenCancel()
          .verify()

    }

  }

  @Nested
  @Disabled
  inner class CircleExperiments {

    @Test
    fun `test succeeds`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
          .uri("/smoke-test?testMode=SUCCEED")
          .accept(TEXT_EVENT_STREAM)
          .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
          .exchange()
          .expectStatus().isOk
          .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
          .expectNext(TestResult("Reset Community test data for X360040"), TestResult("Test triggered"))
          .expectNextSequence(inProgressResults(communityService.maxTestPollCount.toInt(), "SUCCEED"))
          .expectNext(TestResult("Test has completed successfully", true))
          .thenCancel()
          .verify()

    }

    @Test
    fun `test fails`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
          .uri("/smoke-test?testMode=FAIL")
          .accept(TEXT_EVENT_STREAM)
          .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
          .exchange()
          .expectStatus().isOk
          .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
          .expectNext(TestResult("Reset Community test data for X360040"), TestResult("Test triggered"))
          .expectNextSequence(inProgressResults(communityService.maxTestPollCount.toInt(), "FAIL"))
          .expectNext(TestResult("Test has failed", false))
          .thenCancel()
          .verify()

    }

    @Test
    fun `test times out`() {
      val results: FluxExchangeResult<TestResult> = webTestClient.post()
          .uri("/smoke-test?testMode=TIMEOUT")
          .accept(TEXT_EVENT_STREAM)
          .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
          .exchange()
          .expectStatus().isOk
          .returnResult(TestResult::class.java)

      StepVerifier.create(results.responseBody)
          .expectNext(TestResult("Reset Community test data for X360040"), TestResult("Test triggered"))
          .expectNextSequence(inProgressResults(9, "TIMEOUT"))
          .expectNext(TestResult("Test still running (testMode=TIMEOUT)"))
          .thenCancel()
          .verify()

    }

  }

}

private fun inProgressResults(count: Int, testMode: String): List<TestResult> {
  val results = mutableListOf<TestResult>()
  repeat(count) {
    results.add(TestResult("Test still running (testMode=$testMode)"))
  }
  return results.toList()
}
