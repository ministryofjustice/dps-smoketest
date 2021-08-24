package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.CommunityApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.SUCCESS
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PtpuSmokeTestIntegrationTest : IntegrationTestBase() {

  @Nested
  @DisplayName("Ptpu API tests")
  inner class ApiTests {
    @Test
    fun `requires valid authentication token`() {
      webTestClient.post()
        .uri("/smoke-test/prison-to-probation-update/PTPU_T3")
        .accept(TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.post()
        .uri("/smoke-test/prison-to-probation-update/PTPU_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `requires valid test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/prison-to-probation-update/NOT_A_TEST_PROFILE")
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
      val results = webTestClient.post()
        .uri("/smoke-test/prison-to-probation-update/PTPU_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(String::class.java)

      StepVerifier.create(results.responseBody).expectNextCount(1)
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When get test inputs fails")
  inner class WhenGetTestInputsFails {
    @BeforeEach
    internal fun setUp() {
      stubGetTestInputs(HTTP_NOT_FOUND)
    }

    @Test
    fun `fails after first step`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult ->
          testResult.description.contains("Unable to gather the test inputs due to exception ") &&
            testResult.progress == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When reset test fails")
  inner class WhenResetFails {
    @BeforeEach
    internal fun setUp() {
      stubGetTestInputs()
      stubResetTestData(HTTP_NOT_FOUND)
    }

    @Test
    fun `fails after first step`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Retrieved test inputs") }
        .expectNext(TestStatus("Reset Community test failed. The offender X360040 can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When trigger test fails")
  inner class WhenTriggerFails {
    @BeforeEach
    internal fun setUp() {
      stubGetTestInputs()
      stubResetTestData()
      stubTriggerTest(HTTP_NOT_FOUND)
    }

    @Test
    fun `fails after second step`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Retrieved test inputs") }
        .expectNext(TestStatus("Reset Community test data for X360040"))
        .expectNext(TestStatus("Trigger test failed. The offender A7742DY can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test never completes")
  inner class TestNeverCompletes {
    @BeforeEach
    internal fun setUp() {
      stubGetTestInputs()
      stubResetTestData()
      stubTriggerTest()
      stubTestNeverCompletes()
    }

    @Test
    fun `fails on result not found`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Retrieved test inputs") }
        .expectNext(TestStatus("Reset Community test data for X360040"))
        .expectNext(TestStatus("Triggered test for A7742DY"))
        .expectNextSequence(List(9) { TestStatus("Still waiting for offender A7742DY with booking 38479A to be updated") })
        .expectNext(TestStatus("Waiting for final update", COMPLETE))
        .expectNextMatches { testResult ->
          testResult.description.contains("Check test results for A7742DY failed due to 404 Not Found") &&
            testResult.progress == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When check test results fails")
  inner class ResultsNotFound {
    @BeforeEach
    internal fun setUp() {
      stubGetTestInputs()
      stubResetTestData()
      stubTriggerTest()
      stubTestComplete()
      stubGetCustodyDetails(HTTP_NOT_FOUND)
    }

    @Test
    fun `fails on result not found`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Retrieved test inputs") }
        .expectNext(TestStatus("Reset Community test data for X360040"))
        .expectNext(TestStatus("Triggered test for A7742DY"))
        .expectNext(TestStatus("Still waiting for offender A7742DY with booking 38479A to be updated"))
        .expectNext(TestStatus("Test for offender A7742DY with booking 38479A has completed", COMPLETE))
        .expectNext(TestStatus("Waiting for final update", COMPLETE))
        .expectNextMatches { testResult ->
          testResult.description.contains("Check test results for A7742DY failed due to 404 Not Found") &&
            testResult.progress == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test result data incorrect")
  inner class TestStatusDataBad {
    @BeforeEach
    internal fun setUp() {
      stubGetTestInputs()
      stubResetTestData()
      stubTriggerTest()
      stubTestComplete()
      stubGetCustodyDetails(
        body =
        """
          { "bookingNumber": "38479A",
            "institution": { "nomsPrisonInstitutionCode": "WRONG_PRISON" },
            "status": { "code": "D" }
          }
        """.trimIndent()
      )
    }

    @Test
    fun `fails with incorrect custody details`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Retrieved test inputs") }
        .expectNext(TestStatus("Reset Community test data for X360040"))
        .expectNext(TestStatus("Triggered test for A7742DY"))
        .expectNext(TestStatus("Still waiting for offender A7742DY with booking 38479A to be updated"))
        .expectNext(TestStatus("Test for offender A7742DY with booking 38479A has completed", COMPLETE))
        .expectNext(TestStatus("Waiting for final update", COMPLETE))
        .expectNextMatches { testResult ->
          testResult.description.contains("Test for offender A7742DY with booking 38479A failed with custodyDetails") &&
            testResult.progress == FAIL
        }
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test succeeds")
  inner class TestSucceeds {
    @BeforeEach
    internal fun setUp() {
      stubGetTestInputs()
      stubResetTestData()
      stubTriggerTest()
      stubTestComplete()
      stubGetCustodyDetails(
        body =
        """
          { "bookingNumber": "38479A",
            "institution": { "nomsPrisonInstitutionCode": "MDI" },
            "status": { "code": "D" }
          }
        """.trimIndent()
      )
    }

    @Test
    fun `correct events are returned`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNextMatches { testResult -> testResult.description.contains("Retrieved test inputs") }
        .expectNext(TestStatus("Reset Community test data for X360040"))
        .expectNext(TestStatus("Triggered test for A7742DY"))
        .expectNext(TestStatus("Still waiting for offender A7742DY with booking 38479A to be updated"))
        .expectNext(TestStatus("Test for offender A7742DY with booking 38479A has completed", COMPLETE))
        .expectNext(TestStatus("Waiting for final update", COMPLETE))
        .expectNext(TestStatus("Test for offender A7742DY with booking 38479A finished successfully", SUCCESS))
        .verifyComplete()
    }
  }

  private fun postStartTest(): FluxExchangeResult<TestStatus> =
    webTestClient.post()
      .uri("/smoke-test/prison-to-probation-update/PTPU_T3")
      .accept(TEXT_EVENT_STREAM)
      .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
      .exchange()
      .expectStatus().isOk
      .returnResult(TestStatus::class.java)

  private fun stubGetTestInputs(status: Int = HTTP_OK) =
    PrisonApiExtension.prisonApi.stubFor(
      WireMock.get(WireMock.anyUrl()).willReturn(
        WireMock.aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withStatus(status)
          .withBody(
            """
            { "bookingNo": "38479A", "agencyId": "MDI" }
            """.trimIndent()
          )
      )
    )

  private fun stubResetTestData(status: Int = HTTP_OK) =
    CommunityApiExtension.communityApi.stubFor(
      WireMock.post(WireMock.anyUrl()).willReturn(
        WireMock.aResponse()
          .withStatus(status)
      )
    )

  private fun stubTriggerTest(status: Int = HTTP_OK) =
    PrisonApiExtension.prisonApi.stubFor(
      WireMock.post(WireMock.anyUrl()).willReturn(
        WireMock.aResponse()
          .withStatus(status)
      )
    )

  private fun stubTestComplete() {
    CommunityApiExtension.communityApi.stubFor(
      WireMock.get(WireMock.anyUrl())
        .inScenario("My Scenario")
        .whenScenarioStateIs(STARTED)
        .willReturn(
          WireMock.aResponse()
            .withStatus(HTTP_NOT_FOUND)
        )
        .willSetStateTo("Found")
    )
    CommunityApiExtension.communityApi.stubFor(
      WireMock.get(WireMock.anyUrl())
        .inScenario("My Scenario")
        .whenScenarioStateIs("Found")
        .willReturn(
          WireMock.aResponse()
            .withStatus(HTTP_OK)
        )
        .willSetStateTo("Return results")
    )
  }

  private fun stubTestNeverCompletes() {
    CommunityApiExtension.communityApi.stubFor(
      WireMock.get(WireMock.anyUrl())
        .willReturn(
          WireMock.aResponse()
            .withStatus(HTTP_NOT_FOUND)
        )
    )
  }

  private fun stubGetCustodyDetails(status: Int = HTTP_OK, body: String? = null) =
    CommunityApiExtension.communityApi.stubFor(
      WireMock.get(WireMock.anyUrl())
        .inScenario("My Scenario")
        .whenScenarioStateIs("Return results")
        .willReturn(
          WireMock.aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withStatus(status)
            .withBody(body)
        )
    )
}
