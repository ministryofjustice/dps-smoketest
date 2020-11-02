package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class OpenApiDocsTest : IntegrationTestBase() {

  @Test
  fun `open api docs are available`() {
    webTestClient.get()
        .uri("/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
  }

  @Test
  fun `open api docs redirect to correct page`() {
    webTestClient.get()
        .uri("/swagger-ui.html")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().is3xxRedirection
        .expectHeader().value("Location") { it.contains("/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config") }
  }

  @Test
  fun `the swagger json is valid`() {
    webTestClient.get()
        .uri("/v3/api-docs")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("messages").doesNotExist()
  }
}
