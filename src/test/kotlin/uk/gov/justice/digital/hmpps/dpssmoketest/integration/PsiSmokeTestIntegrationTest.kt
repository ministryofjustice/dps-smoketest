package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL

class PsiSmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Nested
  @DisplayName("API tests")
  inner class ApiTests {
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
      webTestClient.post()
        .uri("/smoke-test/probation-search-indexer/PSI_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
    }
  }
}
