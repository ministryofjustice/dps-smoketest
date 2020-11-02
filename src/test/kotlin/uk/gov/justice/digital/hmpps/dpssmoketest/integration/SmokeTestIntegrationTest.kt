package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper

class SmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Test
  fun `requires valid authentication token`() {
    webTestClient.post()
        .uri("/smoke-test")
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
  }

  @Test
  fun `requires correct role`() {
    webTestClient.post()
        .uri("/smoke-test")
        .accept(APPLICATION_JSON)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf()))
        .exchange()
        .expectStatus().isForbidden
  }

  @Test
  fun `succeeds with correct access`() {
    webTestClient.post()
        .uri("/smoke-test")
        .accept(APPLICATION_JSON)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
  }
}