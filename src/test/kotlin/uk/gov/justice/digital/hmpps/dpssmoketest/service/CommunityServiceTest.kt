package uk.gov.justice.digital.hmpps.dpssmoketest.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.IntegrationTestBase
import java.net.HttpURLConnection.HTTP_OK

class CommunityServiceTest: IntegrationTestBase() {

  @Autowired
  lateinit var service: CommunityService

  @Nested
  inner class ResetTestData {
    @Test
    fun `will call custody reset for offender`() {
      communityApiMockServer.stubFor(post(anyUrl()).willReturn(aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(HTTP_OK)))


      service.resetTestData("X12345")

      communityApiMockServer.verify(postRequestedFor(urlEqualTo("/secure/smoketest/offenders/crn/X12345/custody/reset"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")))
    }

  }
}