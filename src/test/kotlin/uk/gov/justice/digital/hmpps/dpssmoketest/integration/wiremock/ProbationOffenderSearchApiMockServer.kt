
package uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ProbationOffenderSearchExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val probationOffenderSearch = ProbationOffenderSearchMockServer()
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
class ProbationOffenderSearchMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8097
  }
}
