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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.CommunityApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.INCOMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.SUCCESS
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
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
        )
      )

      service.resetTestData("X12345").block()

      CommunityApiExtension.communityApi.verify(
        postRequestedFor(urlEqualTo("/secure/smoketest/offenders/crn/X12345/custody/reset"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("Bearer ABCDE"))
      )
    }

    @Test
    fun `will return a non fail test result`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
        )
      )

      assertThat(service.resetTestData("X12345").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when test data not found`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_NOT_FOUND)
        )
      )

      assertThat(service.resetTestData("X12345").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR)
        )
      )

      assertThat(service.resetTestData("X12345").block()?.progress).isEqualTo(FAIL)
    }
  }

  @Nested
  inner class ChecksTestData {
    @Test
    fun `will call to check custody data exists`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
        )
      )

      service.checkTestComplete("A7742DY", "38479A").block()

      CommunityApiExtension.communityApi.verify(
        getRequestedFor(urlEqualTo("/secure/offenders/nomsNumber/A7742DY/custody/bookingNumber/38479A"))
          .withHeader("Authorization", equalTo("Bearer ABCDE"))
      )
    }

    @Test
    fun `will offender is found return that test is complete`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
        )
      )

      assertThat(service.checkTestComplete("X12345", "38479A").block()?.progress).isEqualTo(COMPLETE)
    }

    @Test
    fun `will offender is not found return that test is not complete`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_NOT_FOUND)
        )
      )

      assertThat(service.checkTestComplete("X12345", "38479A").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR)
        )
      )

      assertThat(service.checkTestComplete("X12345", "38479A").block()?.progress).isEqualTo(FAIL)
    }
  }

  @Nested
  inner class AssertTestStatus {

    @Test
    fun `will fail on not found`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withStatus(HTTP_NOT_FOUND)
        )
      )

      assertThat(service.assertTestResult("X12345", "38479A", "MDI").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will fail on server error`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withStatus(HTTP_INTERNAL_ERROR)
        )
      )

      assertThat(service.assertTestResult("X12345", "38479A", "MDI").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will succeed with matching data`() {
      CommunityApiExtension.communityApi.stubFor(
        get(urlEqualTo("/secure/offenders/nomsNumber/X12345/custody/bookingNumber/38479A")).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withBody(
              """
              { "bookingNumber": "38479A",
                "institution": { "nomsPrisonInstitutionCode": "MDI" },
                "status": { "code": "D" }
              }
              """.trimIndent()
            )
        )
      )

      assertThat(service.assertTestResult("X12345", "38479A", "MDI").block()?.progress).isEqualTo(SUCCESS)
    }

    @ParameterizedTest
    @CsvSource(
      "X12345, 38479A, WRONG",
      "X12345, WRONG,  MDI",
      "WRONG,  38479A, MDI",
    )
    fun `will fail with bad data`(
      nomsNumber: String,
      bookingNumber: String,
      prisonCode: String
    ) {
      CommunityApiExtension.communityApi.stubFor(
        get(urlEqualTo("/secure/offenders/nomsNumber/X12345/custody/bookingNumber/38479A")).willReturn(
          aResponse()
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withStatus(HTTP_OK)
            .withBody(
              """
              { "bookingNumber": "38479A",
                "institution": { "nomsPrisonInstitutionCode": "MDI" },
                "status": { "code": "D" }
              }
              """.trimIndent()
            )
        )
      )

      assertThat(service.assertTestResult(nomsNumber, bookingNumber, prisonCode).block()?.progress).isEqualTo(FAIL)
    }
  }
}
