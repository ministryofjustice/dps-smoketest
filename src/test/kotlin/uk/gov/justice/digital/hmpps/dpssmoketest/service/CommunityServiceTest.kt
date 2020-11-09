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
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.CommunityApiExtension
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

class CommunityServiceTest : IntegrationTestBase() {

  @Autowired
  lateinit var service: CommunityService

  @Nested
  inner class ResetTestData {
    @Test
    fun `will call custody reset for offender`() {
      CommunityApiExtension.communityApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_OK)))

      service.resetTestData("X12345")

      CommunityApiExtension.communityApi.verify(postRequestedFor(urlEqualTo("/secure/smoketest/offenders/crn/X12345/custody/reset"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")))
    }

    @Test
    fun `will return a non fail test result`() {
      CommunityApiExtension.communityApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_OK)))

      assertThat(service.resetTestData("X12345").outcome).isNull()
    }

    @Test
    fun `will return a fail test result when test data not found`() {
      CommunityApiExtension.communityApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_NOT_FOUND)))

      assertThat(service.resetTestData("X12345").outcome).isFalse
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      CommunityApiExtension.communityApi.stubFor(post(anyUrl()).willReturn(aResponse()
          .withStatus(HTTP_INTERNAL_ERROR)))

      assertThat(service.resetTestData("X12345").outcome).isFalse
    }
  }
}