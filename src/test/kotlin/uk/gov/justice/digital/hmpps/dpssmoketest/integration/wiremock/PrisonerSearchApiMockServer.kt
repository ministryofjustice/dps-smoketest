package uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import com.google.gson.Gson
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonerSearchExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearch = PrisonerSearchMockServer(
      wireMockConfig().extensions(SearchBodyTransformer::class.java),
    )
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearch.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearch.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearch.stop()
  }
}

class PrisonerSearchMockServer(config: WireMockConfiguration) : WireMockServer(config.port(WIREMOCK_PORT)) {
  companion object {
    private const val WIREMOCK_PORT = 8097
  }
}

class SearchBodyTransformer : ResponseDefinitionTransformerV2 {
  override fun transform(serveEvent: ServeEvent): ResponseDefinition {
    with(serveEvent) {
      val requestBody: SearchRequest = Gson().fromJson(request.bodyAsString, SearchRequest::class.java)
      // echo request main attributes in body to simulate perfect match
      return ResponseDefinitionBuilder()
        .withHeaders(responseDefinition.headers)
        .withStatus(responseDefinition.status)
        .withBody(
          """
            {
              "content": [
                {
                  "prisonerNumber": "${requestBody.prisonerIdentifier}",
                  "firstName": "${requestBody.firstName}",
                  "lastName": "${requestBody.lastName}"
                }
              ]
            }
          """.trimIndent(),
        )
        .build()
    }
  }

  override fun getName(): String = "search-body"

  override fun applyGlobally(): Boolean = false
}

private data class SearchRequest(val prisonerIdentifier: String, val firstName: String, val lastName: String)
