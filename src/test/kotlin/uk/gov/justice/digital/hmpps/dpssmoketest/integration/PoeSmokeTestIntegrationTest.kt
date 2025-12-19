package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.apache.http.HttpHeaders.CONTENT_TYPE
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.INCOMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.SUCCESS
import uk.gov.justice.digital.hmpps.dpssmoketest.service.PoeTestProfiles
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PoeSmokeTestIntegrationTest : IntegrationTestBase() {

  val poeOffenderNo = PoeTestProfiles.POE_T3.profile.nomsNumber

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
        .headers(jwtAuthHelper.setAuthorisationHeader(clientId = "dps-smoke-test"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `requires valid test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/prison-offender-events/NOT_A_TEST_PROFILE")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisationHeader(clientId = "dps-smoke-test", roles = listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(TestStatus::class.java)

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Unknown test profile NOT_A_TEST_PROFILE", FAIL))
        .verifyComplete()
    }

    @Disabled
    @Test
    fun `succeeds with correct access and test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/prison-offender-events/POE_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisationHeader(clientId = "dps-smoke-test", roles = listOf("ROLE_SMOKE_TEST")))
        .exchange()
        .expectStatus().isOk
        .returnResult(String::class.java)

      StepVerifier.create(results.responseBody).expectNextCount(6)
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When trigger test fails for release event")
  inner class WhenTriggerFailsForReleaseEvent {
    @BeforeEach
    internal fun setUp() {
      stubTriggerTest(HTTP_NOT_FOUND)
      stubPrisonerStatus()
    }

    @Test
    fun `status reflects error`() {
      val results = postStartTest()
      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Will release prisoner $poeOffenderNo", INCOMPLETE))
        .expectNext(TestStatus("Trigger test failed. The offender $poeOffenderNo can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When trigger test fails for recall")
  inner class WhenTriggerFailsForRecallEvent {
    @BeforeEach
    internal fun setUp() {
      stubReleaseTriggerTest(HTTP_OK)
      stubPrisonerStatus()
      stubRecallTriggerTest(HTTP_NOT_FOUND)
      doNothing().whenever(queueService).purgeQueue()
      hmppsEventQueue.sqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(hmppsEventQueueUrl).messageBody("/messages/prisonerReleased".loadJson()).build(),
      ).get()
      await untilCallTo { hmppsEventQueueSqsClient.countMessagesOnQueue(hmppsEventQueueUrl).get() } matches { it == 1 }
    }

    @Test
    fun `status reflects error`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Will release prisoner $poeOffenderNo", INCOMPLETE))
        .expectNext(TestStatus("Triggered test for $poeOffenderNo"))
        .expectNext(TestStatus("Test for offender $poeOffenderNo prison-offender-events.prisoner.released event finished successfully", COMPLETE))
        .expectNext(TestStatus("Will recall prisoner $poeOffenderNo", INCOMPLETE))
        .expectNext(TestStatus("Trigger test failed. The offender $poeOffenderNo can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When trigger test fails for prisoner status")
  inner class WhenTriggerFailsForPrisonerStatus {
    @BeforeEach
    internal fun setUp() {
      stubPrisonerStatus(status = HTTP_NOT_FOUND)
    }

    @Test
    fun `status reflects error`() {
      val results = postStartTest()
      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Offender we expected to exist $poeOffenderNo was not found. Check the offender has not be deleted in NOMIS", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @DisplayName("When test succeeds")
  inner class TestSucceeds {
    @BeforeEach
    internal fun setUp() {
      stubTriggerTest()
      stubPrisonerStatus()
      doNothing().whenever(queueService).purgeQueue()
      hmppsEventQueue.sqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(hmppsEventQueueUrl).messageBody("/messages/prisonerReleased".loadJson()).build(),
      ).get()
      await untilCallTo { hmppsEventQueueSqsClient.countMessagesOnQueue(hmppsEventQueueUrl).get() } matches { it == 1 }
      hmppsEventQueue.sqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(hmppsEventQueueUrl).messageBody("/messages/prisonerRecalled".loadJson()).build(),
      ).get()
      await untilCallTo { hmppsEventQueueSqsClient.countMessagesOnQueue(hmppsEventQueueUrl).get() } matches { it == 2 }
    }

    @Test
    fun `status reflects success`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Will release prisoner $poeOffenderNo", INCOMPLETE))
        .expectNext(TestStatus("Triggered test for $poeOffenderNo"))
        .expectNext(TestStatus("Test for offender $poeOffenderNo prison-offender-events.prisoner.released event finished successfully", COMPLETE))
        .expectNext(TestStatus("Will recall prisoner $poeOffenderNo", INCOMPLETE))
        .expectNext(TestStatus("Triggered test for $poeOffenderNo"))
        .expectNext(TestStatus("Test for offender $poeOffenderNo prison-offender-events.prisoner.received event finished successfully", SUCCESS))
        .verifyComplete()
    }

    @Test
    fun `smoke test user passed through to auth`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody).expectNextCount(6).verifyComplete()

      OAuthExtension.oAuthApi.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/oauth/token"))
          .withRequestBody(WireMock.equalTo("grant_type=client_credentials&username=SMOKE_TEST_USER")),
      )
    }
  }

  private fun postStartTest(): FluxExchangeResult<TestStatus> = webTestClient.post()
    .uri("/smoke-test/prison-offender-events/POE_T3")
    .accept(TEXT_EVENT_STREAM)
    .headers(jwtAuthHelper.setAuthorisationHeader(clientId = "dps-smoke-test", roles = listOf("ROLE_SMOKE_TEST")))
    .exchange()
    .expectStatus().isOk
    .returnResult(TestStatus::class.java)

  private fun stubTriggerTest(status: Int = HTTP_OK) = PrisonApiExtension.prisonApi.stubFor(
    WireMock.put(WireMock.anyUrl()).willReturn(
      WireMock.aResponse()
        .withStatus(status),
    ),
  )

  private fun stubReleaseTriggerTest(status: Int = HTTP_OK) = PrisonApiExtension.prisonApi.stubFor(
    WireMock.put("/api/smoketest/offenders/$poeOffenderNo/release").willReturn(
      WireMock.aResponse()
        .withStatus(status),
    ),
  )
  private fun stubRecallTriggerTest(status: Int = HTTP_OK) = PrisonApiExtension.prisonApi.stubFor(
    WireMock.put("/api/smoketest/offenders/$poeOffenderNo/recall").willReturn(
      WireMock.aResponse()
        .withStatus(status),
    ),
  )
}

private fun String.loadJson(): String = PoeSmokeTestIntegrationTest::class.java.getResource("$this.json").readText()

private fun stubPrisonerStatus(offenderNo: String = PoeTestProfiles.POE_T3.profile.nomsNumber, status: Int = HTTP_OK) = PrisonApiExtension.prisonApi.stubFor(
  WireMock.put(WireMock.urlPathMatching("/api/smoketest/offenders/$offenderNo/status")).willReturn(
    WireMock.aResponse()
      .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
      .withStatus(status),
  ),
)
