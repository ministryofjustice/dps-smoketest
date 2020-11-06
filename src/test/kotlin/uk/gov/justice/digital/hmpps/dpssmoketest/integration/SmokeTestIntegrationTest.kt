package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper

class SmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

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
//
//  @Test
//  fun `can receive test results`() {
//    val results: FluxExchangeResult<TestResult> = webTestClient.get()
//        .uri("/start")
//        .accept(MediaType.TEXT_EVENT_STREAM)
//        .exchange()
//        .expectStatus().isOk
//        .returnResult(TestResult::class.java)
//
//    StepVerifier.create(results.responseBody)
//        .expectNext(TestResult(false, "Processed probation event"), TestResult(true, "Updated Delius"))
//        .thenCancel()
//        .verify()
//
//  }
}