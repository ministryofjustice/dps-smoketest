package uk.gov.justice.digital.hmpps.dpssmoketest.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.service.SmokeTestService

@Tag(name = "DPS Smoke Tests")
@RestController
class SmokeTestResource(private val smokeTestService: SmokeTestService) {

  @PostMapping("/smoke-test", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  @PreAuthorize("hasRole('SMOKE_TEST')")
  @Operation(
    summary = "Start a new smoke test"
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token"
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role ROLE_SMOKE_TEST"
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found, the test data could not be found in prison-api or community-api"
      )
    ]
  )
  fun smokeTest(): Flux<TestResult> = smokeTestService.runSmokeTest()

  data class TestResult(val description: String, val outcome: Boolean? = null)
}
