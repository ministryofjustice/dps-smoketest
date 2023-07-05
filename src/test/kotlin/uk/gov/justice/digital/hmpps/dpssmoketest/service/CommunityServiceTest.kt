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
            .withStatus(HTTP_OK),
        ),
      )

      service.resetCustodyTestData("X12345").block()

      CommunityApiExtension.communityApi.verify(
        postRequestedFor(urlEqualTo("/secure/smoketest/offenders/crn/X12345/custody/reset"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")),
      )
    }

    @Test
    fun `will return a non fail test result`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      assertThat(service.resetCustodyTestData("X12345").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when test data not found`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_NOT_FOUND),
        ),
      )

      assertThat(service.resetCustodyTestData("X12345").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR),
        ),
      )

      assertThat(service.resetCustodyTestData("X12345").block()?.progress).isEqualTo(FAIL)
    }
  }

  @Nested
  inner class ChecksTestData {
    @Test
    fun `will call to check custody data exists`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      service.checkCustodyTestComplete("A4799DZ", "45135A").block()

      CommunityApiExtension.communityApi.verify(
        getRequestedFor(urlEqualTo("/secure/offenders/nomsNumber/A4799DZ/custody/bookingNumber/45135A"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")),
      )
    }

    @Test
    fun `will offender is found return that test is complete`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      assertThat(service.checkCustodyTestComplete("X12345", "45135A").block()?.progress).isEqualTo(COMPLETE)
    }

    @Test
    fun `will offender is not found return that test is not complete`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_NOT_FOUND),
        ),
      )

      assertThat(service.checkCustodyTestComplete("X12345", "45135A").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when fails to reset for any reason`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR),
        ),
      )

      assertThat(service.checkCustodyTestComplete("X12345", "45135A").block()?.progress).isEqualTo(FAIL)
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
            .withStatus(HTTP_NOT_FOUND),
        ),
      )

      assertThat(service.assertTestResult("X12345", "45135A", "MDI").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will fail on server error`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withStatus(HTTP_INTERNAL_ERROR),
        ),
      )

      assertThat(service.assertTestResult("X12345", "45135A", "MDI").block()?.progress).isEqualTo(FAIL)
    }

    @Test
    fun `will succeed with matching data`() {
      CommunityApiExtension.communityApi.stubFor(
        get(urlEqualTo("/secure/offenders/nomsNumber/X12345/custody/bookingNumber/45135A")).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withBody(
              """
              { "bookingNumber": "45135A",
                "institution": { "nomsPrisonInstitutionCode": "MDI" },
                "status": { "code": "D" }
              }
              """.trimIndent(),
            ),
        ),
      )

      assertThat(service.assertTestResult("X12345", "45135A", "MDI").block()?.progress).isEqualTo(SUCCESS)
    }

    @ParameterizedTest
    @CsvSource(
      "X12345, 45135A, WRONG",
      "X12345, WRONG,  MDI",
      "WRONG,  45135A, MDI",
    )
    fun `will fail with bad data`(
      nomsNumber: String,
      bookingNumber: String,
      prisonCode: String,
    ) {
      CommunityApiExtension.communityApi.stubFor(
        get(urlEqualTo("/secure/offenders/nomsNumber/X12345/custody/bookingNumber/45135A")).willReturn(
          aResponse()
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withStatus(HTTP_OK)
            .withBody(
              """
              { "bookingNumber": "45135A",
                "institution": { "nomsPrisonInstitutionCode": "MDI" },
                "status": { "code": "D" }
              }
              """.trimIndent(),
            ),
        ),
      )

      assertThat(service.assertTestResult(nomsNumber, bookingNumber, prisonCode).block()?.progress).isEqualTo(FAIL)
    }
  }

  @Nested
  @DisplayName("setOffenderDetailsTestData")
  inner class SetOffenderDetailsTestData {
    @Test
    fun `will call set offender details for offender`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      service.setOffenderDetailsTestData("X12345", "jane", "smith").block()

      CommunityApiExtension.communityApi.verify(
        postRequestedFor(urlEqualTo("/secure/smoketest/offenders/crn/X12345/details"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")),
      )
    }

    @Test
    fun `will return a non fail test result`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      assertThat(service.setOffenderDetailsTestData("X12345", "jane", "smith").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when details can not be set`() {
      CommunityApiExtension.communityApi.stubFor(
        post(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR),
        ),
      )

      assertThat(service.setOffenderDetailsTestData("X12345", "jane", "smith").block()?.progress).isEqualTo(FAIL)
    }
  }

  @Nested
  @DisplayName("checkOffenderExists")
  inner class CheckOffenderExists {
    @Test
    fun `will call get offender details for offender`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK),
        ),
      )

      service.checkOffenderExists("X12345").block()

      CommunityApiExtension.communityApi.verify(
        getRequestedFor(urlEqualTo("/secure/offenders/crn/X12345"))
          .withHeader("Authorization", equalTo("Bearer ABCDE")),
      )
    }

    @Test
    fun `will return a non fail test result`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_OK)
            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .withBody(
              """
                {
                "firstName": "Jane",
                "surname": "Smith"
                }
              """.trimIndent(),
            ),
        ),
      )

      assertThat(service.checkOffenderExists("X12345").block()?.progress).isEqualTo(INCOMPLETE)
    }

    @Test
    fun `will return a fail test result when details can not be retrieved`() {
      CommunityApiExtension.communityApi.stubFor(
        get(anyUrl()).willReturn(
          aResponse()
            .withStatus(HTTP_INTERNAL_ERROR),
        ),
      )

      assertThat(service.checkOffenderExists("X12345").block()?.progress).isEqualTo(FAIL)
    }
  }
}
