package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
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
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND

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

  private fun postStartTest(): FluxExchangeResult<TestStatus> =
    webTestClient.post()
      .uri("/smoke-test/probation-search-indexer/PSI_T3")
      .accept(TEXT_EVENT_STREAM)
      .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
      .exchange()
      .expectStatus().isOk
      .returnResult(TestStatus::class.java)

}

private fun stubCheckOffenderExists(status: Int = HttpURLConnection.HTTP_OK) =
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

private fun stubChangeOffenderName(status: Int = HttpURLConnection.HTTP_OK) =
  CommunityApiExtension.communityApi.stubFor(
    WireMock.post(WireMock.anyUrl()).willReturn(
      WireMock.aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(status)
    )
  )

