package uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ProbationOffenderSearchExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val probationOffenderSearch = ProbationOffenderSearchMockServer(
      wireMockConfig().extensions(SearchBodyTransformer::class.java)
    )
  }

  override fun beforeAll(context: ExtensionContext?) {
    probationOffenderSearch.start()
  }

  override fun beforeEach(context: ExtensionContext?) {
    probationOffenderSearch.resetAll()
  }

  override fun afterAll(context: ExtensionContext?) {
    probationOffenderSearch.stop()
  }
}

class ProbationOffenderSearchMockServer(config: WireMockConfiguration) : WireMockServer(config.port(WIREMOCK_PORT)) {
  companion object {
    private const val WIREMOCK_PORT = 8097
  }
}

class SearchBodyTransformer : ResponseDefinitionTransformer() {
  override fun transform(request: Request, responseDefinition: ResponseDefinition, files: FileSource?, parameters: Parameters?): ResponseDefinition {
    val requestBody: SearchRequest = Gson().fromJson(request.bodyAsString, SearchRequest::class.java)
    // echo request main attributes in body to simulate perfect match
    return ResponseDefinitionBuilder()
      .withHeaders(responseDefinition.headers)
      .withStatus(responseDefinition.status)
      .withBody(
        """
            [
             {
                "otherIds": {
                  "crn": "${requestBody.crn}"
                },
                "firstName": "${requestBody.firstName}",
                "surname": "${requestBody.surname}"
             }
            ]
        """.trimIndent()
      )
      .build()
  }

  override fun getName(): String {
    return "search-body"
  }

  override fun applyGlobally(): Boolean {
    return false
  }
}

private data class SearchRequest(val crn: String, val firstName: String, val surname: String)
