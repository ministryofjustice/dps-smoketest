package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult

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
  inner class TestOutcomeScenarios {

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
          .expectNext(TestResult("Reset Community test data"), TestResult("Test triggered"))
          .expectNextSequence(inProgressResults(4, "SUCCEED"))
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
          .expectNext(TestResult("Reset Community test data"), TestResult("Test triggered"))
          .expectNextSequence(inProgressResults(4, "FAIL"))
          .expectNext(TestResult("Test has failed", true))
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
          .expectNext(TestResult("Reset Community test data"), TestResult("Test triggered"))
          .expectNextSequence(inProgressResults(9, "TIMEOUT"))
          .expectNext(TestResult("Test still running (testMode=TIMEOUT)"))
          .thenCancel()
          .verify()

    }

    private fun inProgressResults(count: Int, testMode: String): List<TestResult> {
      val results = mutableListOf<TestResult>()
      repeat(count) {
        results.add(TestResult("Test still running (testMode=$testMode)"))
      }
      return results.toList()
    }
  }


}