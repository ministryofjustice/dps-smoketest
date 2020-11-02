package uk.gov.justice.digital.hmpps.dpssmoketest.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "DPS Smoke Tests")
@RestController
class SmokeTestResource {

  @PostMapping("/smoke-test")
  @PreAuthorize("hasRole('SMOKE_TEST')")
  @Operation(
      summary = "Start a new smoke test"
  )
  @ApiResponses(value = [
    ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token"
    ),
    ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role ROLE_SMOKE_TEST"
    )
  ])
  fun smokeTest() {

  }

}