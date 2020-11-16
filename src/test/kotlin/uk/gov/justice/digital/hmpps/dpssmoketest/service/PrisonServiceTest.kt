package uk.gov.justice.digital.hmpps.dpssmoketest.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.INCOMPLETE
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PrisonServiceTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: PrisonService

  @Nested
  @DisplayName("Get test inputs")
  inner class GetTestInputs {
    @Test
    internal fun `will call get imprisonment status for the offender`() {
      PrisonApiExtension.prisonApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withBody(
              """
              {"bookingNo": "38479A", "agencyId": "MDI" }
              """.trimIndent()
            )
        )
      )

      service.getTestInputs("A7742DY", "X360040").block()

      PrisonApiExtension.prisonApi.verify(
        getRequestedFor(urlEqualTo("/api/smoketest/offenders/A7742DY/imprisonment-status"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("Bearer ABCDE"))
      )
    }

    @Test
    internal fun `will return offender imprisonment status and in progress test status`() {
      PrisonApiExtension.prisonApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withBody(
              """
              {"bookingNo": "38479A", "agencyId": "MDI" }
              """.trimIndent()
            )
        )
      )

      val result = service.getTestInputs("A7742DY", "X360040").block() as PtpuTestInputs

      assertThat(result.testStatus.progress == INCOMPLETE)
      assertThat(result.nomsNumber).isEqualTo("A7742DY")
      assertThat(result.crn).isEqualTo("X360040")
      assertThat(result.bookingNumber).isEqualTo("38479A")
      assertThat(result.prisonCode).isEqualTo("MDI")
    }

    @Test
    internal fun `will fail if the offender is not found`() {
      PrisonApiExtension.prisonApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_NOT_FOUND)
        )
      )

      val result = service.getTestInputs("A7742DY", "X360040").block() as PtpuTestInputs

      assertThat(result.testStatus.progress == FAIL)
      assertThat(result.nomsNumber).isEqualTo("A7742DY")
      assertThat(result.crn).isEqualTo("X360040")
      assertThat(result.bookingNumber).isEqualTo("NOT FOUND")
      assertThat(result.prisonCode).isEqualTo("NOT FOUND")
    }

    @Test
    internal fun `will fail if there was a server error`() {
      PrisonApiExtension.prisonApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR)
        )
      )

      val result = service.getTestInputs("A7742DY", "X360040").block() as PtpuTestInputs

      assertThat(result.testStatus.progress == FAIL)
      assertThat(result.nomsNumber).isEqualTo("A7742DY")
      assertThat(result.crn).isEqualTo("X360040")
      assertThat(result.bookingNumber).isEqualTo("NOT FOUND")
      assertThat(result.prisonCode).isEqualTo("NOT FOUND")
    }
  }

  @Nested
  @DisplayName("Trigger test")
  inner class TriggerTest {
    @Test
    fun `will call set imprisonment status for the offender`() {
      PrisonApiExtension.prisonApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
        )
      )

      service.triggerTest("A7742DY").block()

      PrisonApiExtension.prisonApi.verify(
        postRequestedFor(urlEqualTo("/api/smoketest/offenders/A7742DY/imprisonment-status"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("Bearer ABCDE"))
      )
    }

    @Test
    fun `will return a non fail test result`() {
      PrisonApiExtension.prisonApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
        )
      )

      assertThat(service.triggerTest("A7742DY").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when test data not found`() {
      PrisonApiExtension.prisonApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_NOT_FOUND)
        )
      )

      assertThat(service.triggerTest("A7742DY").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      PrisonApiExtension.prisonApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR)
        )
      )

      assertThat(service.triggerTest("A7742DY").block()?.progress).isEqualTo(FAIL)
    }
  }
}
