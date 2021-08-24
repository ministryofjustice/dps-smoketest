package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.web.reactive.server.FluxExchangeResult
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.INCOMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.SUCCESS
import uk.gov.justice.digital.hmpps.dpssmoketest.service.QueueService
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PoeSmokeTestIntegrationTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @SpyBean
  protected lateinit var queueService: QueueService

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

    @Disabled
    @Test
    fun `succeeds with correct access and test profile`() {
      val results = webTestClient.post()
        .uri("/smoke-test/prison-offender-events/POE_T3")
        .accept(TEXT_EVENT_STREAM)
        .headers(jwtAuthHelper.setAuthorisation("dps-smoke-test", listOf("ROLE_SMOKE_TEST")))
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
  @DisplayName("When trigger test fails for recall")
  inner class WhenTriggerFailsForRecallEvent {
    @BeforeEach
    internal fun setUp() {
      stubReleaseTriggerTest(HTTP_OK)
      stubRecallTriggerTest(HTTP_NOT_FOUND)
      doNothing().whenever(queueService).purgeQueue()
      hmppsEventQueue.sqsClient.sendMessage(hmppsEventQueueUrl, "/messages/prisonerReleased".loadJson())
      await untilCallTo { hmppsEventQueueSqsClient.numMessages(hmppsEventQueueUrl) } matches { it == 1 }
    }

    @Disabled
    @Test
    fun `status reflects error`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Will release prisoner A7851DY", INCOMPLETE))
        .expectNext(TestStatus("Triggered test for A7851DY"))
        .expectNext(TestStatus("Test for offender A7851DY prison-offender-events.prisoner.released event finished successfully", COMPLETE))

        .expectNext(TestStatus("Will recall prisoner A7851DY", INCOMPLETE))
        .expectNext(TestStatus("Trigger test failed. The offender A7851DY can not be found", FAIL))
        .verifyComplete()
    }
  }

  @Nested
  @Disabled
  @DisplayName("When test succeeds")
  inner class TestSucceeds {
    @BeforeEach
    internal fun setUp() {
      stubTriggerTest()
      doNothing().whenever(queueService).purgeQueue()
      hmppsEventQueue.sqsClient.sendMessage(hmppsEventQueueUrl, "/messages/prisonerReleased".loadJson())
      await untilCallTo { hmppsEventQueueSqsClient.numMessages(hmppsEventQueueUrl) } matches { it == 1 }
      hmppsEventQueue.sqsClient.sendMessage(hmppsEventQueueUrl, "/messages/prisonerRecalled".loadJson())
      await untilCallTo { hmppsEventQueueSqsClient.numMessages(hmppsEventQueueUrl) } matches { it == 2 }
    }

    @Test
    fun `status reflects success`() {
      val results = postStartTest()

      StepVerifier.create(results.responseBody)
        .expectNext(TestStatus("Will release prisoner A7851DY", INCOMPLETE))
        .expectNext(TestStatus("Triggered test for A7851DY"))
        .expectNext(TestStatus("Test for offender A7851DY prison-offender-events.prisoner.released event finished successfully", COMPLETE))

        .expectNext(TestStatus("Will recall prisoner A7851DY", INCOMPLETE))
        .expectNext(TestStatus("Triggered test for A7851DY"))
        .expectNext(TestStatus("Test for offender A7851DY prison-offender-events.prisoner.received event finished successfully", SUCCESS))

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

  private fun stubReleaseTriggerTest(status: Int = HTTP_OK) =
    PrisonApiExtension.prisonApi.stubFor(
      WireMock.put("/api/smoketest/offenders/A7851DY/release").willReturn(
        WireMock.aResponse()
          .withStatus(status)
      )
    )
  private fun stubRecallTriggerTest(status: Int = HTTP_OK) =
    PrisonApiExtension.prisonApi.stubFor(
      WireMock.put("/api/smoketest/offenders/A7851DY/recall").willReturn(
        WireMock.aResponse()
          .withStatus(status)
      )
    )
}
private fun String.loadJson(): String {
  return PoeSmokeTestIntegrationTest::class.java.getResource("$this.json").readText()
}
