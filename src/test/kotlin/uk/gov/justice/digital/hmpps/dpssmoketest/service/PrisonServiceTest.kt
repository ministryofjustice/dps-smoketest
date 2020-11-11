package uk.gov.justice.digital.hmpps.dpssmoketest.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class PrisonServiceTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: PrisonService

  @Nested
  inner class TriggerTest {
    @Test
    fun `will call set imprisonment status for the offender`() {
      PrisonApiExtension.prisonApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_OK)))

      service.triggerTest("A7742DY").block()

      PrisonApiExtension.prisonApi.verify(postRequestedFor(urlEqualTo("/api/smoketest/offenders/A7742DY/imprisonment-status"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")))
    }

    @Test
    fun `will return a non fail test result`() {
      PrisonApiExtension.prisonApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_OK)))

      assertThat(service.triggerTest("A7742DY").block()?.outcome).isNull()
    }

    @Test
    fun `will return a fail test result when test data not found`() {
      PrisonApiExtension.prisonApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_NOT_FOUND)))

      assertThat(service.triggerTest("A7742DY").block()?.outcome).isFalse
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      PrisonApiExtension.prisonApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_INTERNAL_ERROR)))

      assertThat(service.triggerTest("A7742DY").block()?.outcome).isFalse
    }
  }
}