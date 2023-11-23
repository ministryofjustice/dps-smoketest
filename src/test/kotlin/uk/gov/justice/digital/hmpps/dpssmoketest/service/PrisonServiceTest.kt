package uk.gov.justice.digital.hmpps.dpssmoketest.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension.Companion.prisonApi
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.INCOMPLETE
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PrisonServiceTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: PrisonService

  @Nested
  @DisplayName("Trigger test")
  inner class TriggerTest {
    @Test
    fun `will call recall for the offender`() {
      prisonApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      service.triggerPoeRecallTest("A4799DZ").block()

      prisonApi.verify(
        putRequestedFor(urlEqualTo("/api/smoketest/offenders/A4799DZ/recall"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")),
      )
    }

    @Test
    fun `will return a non fail test result`() {
      prisonApi.stubFor(
        put(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      assertThat(service.triggerPoeRecallTest("A4799DZ").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when test data not found`() {
      prisonApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_NOT_FOUND),
        ),
      )

      assertThat(service.triggerPoeRecallTest("A4799DZ").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      prisonApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR),
        ),
      )

      assertThat(service.triggerPoeRecallTest("A4799DZ").block()?.progress).isEqualTo(FAIL)
    }
  }
}
