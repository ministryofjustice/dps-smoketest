package uk.gov.justice.digital.hmpps.dpssmoketest.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.digital.hmpps.dpssmoketest.service.PoeSmokeTestService
import uk.gov.justice.digital.hmpps.dpssmoketest.service.PoeTestProfiles
import uk.gov.justice.digital.hmpps.dpssmoketest.service.PsiSmokeTestService
import uk.gov.justice.digital.hmpps.dpssmoketest.service.PsiTestProfiles

@Tag(name = "DPS Smoke Tests")
@RestController
class SmokeTestResource(
  private val psiSmokeTestService: PsiSmokeTestService,
  private val poeSmokeTestService: PoeSmokeTestService,

) {

  @PostMapping("/smoke-test/prisoner-search/{testProfile}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  @PreAuthorize("hasRole('SMOKE_TEST')")
  @Operation(
    summary = "Start a new smoke test for prisoner search and prisoner search indexer",
    description =
    """   This tests the prisoner-search & indexer happy path, which means the following scenarios are working:
          Events are generated by Prison Offender Events
          hmpps-auth is providing tokens
          prisoner-search-indexer is indexing
          prisoner-search is working
       """,
    security = [SecurityRequirement(name = "smoke-test-security-scheme")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role ROLE_SMOKE_TEST",
      ),
    ],
  )
  fun smokeTestPrisonerSearch(
    @Parameter(
      name = "testProfile",
      description = "The profile that provides the test data",
      example = "PSI_T3",
      required = true,
    )
    @NotNull
    @PathVariable(value = "testProfile")
    testProfile: String,
  ): Flux<TestStatus> = runCatching { PsiTestProfiles.valueOf(testProfile).profile }
    .map { profile ->
      psiSmokeTestService.runSmokeTest(profile).concatMap {
        if (it.hasResult()) psiSmokeTestService.cleanup(profile).thenReturn(it) else Flux.just(it)
      }
    }.getOrDefault(Flux.just(TestStatus("Unknown test profile $testProfile", FAIL)))

  @PostMapping("/smoke-test/prison-offender-events/{testProfile}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  @PreAuthorize("hasRole('SMOKE_TEST')")
  @Operation(
    summary = "Start a new smoke test for prison offender events",
    description =
    """   This tests the prison offender events happy path, which means the following scenarios are working:
          Prison Events are generated by Prison Offender Events
          HMPPS Domain Events are generated by Prison Offender Events
          hmpps-auth is providing tokens to access prison-api
          prison-api allows us to query Nomis
        """,
    security = [SecurityRequirement(name = "smoke-test-security-scheme")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role ROLE_SMOKE_TEST",
      ),
    ],
  )
  fun smokeTestPoe(
    @Parameter(
      name = "testProfile",
      description = "The profile that provides the test data",
      example = "POE_T3",
      required = true,
    )
    @NotNull
    @PathVariable(value = "testProfile")
    testProfile: String,
  ): Flux<TestStatus> = runCatching { PoeTestProfiles.valueOf(testProfile).profile }
    .map { poeSmokeTestService.runSmokeTest(it) }
    .getOrDefault(Flux.just(TestStatus("Unknown test profile $testProfile", FAIL)))

  @Schema(description = "One of a sequence test statuses. The last status should have progress SUCCESS or FAIL if the test concluded.")
  data class TestStatus(
    @Schema(description = "Human readable description of the latest test status")
    val description: String,
    @Schema(description = "The current progress of the test")
    val progress: TestProgress = TestProgress.INCOMPLETE,
  ) {
    fun testComplete() = this.progress != TestProgress.INCOMPLETE
    fun hasResult() = this.progress == TestProgress.SUCCESS || this.progress == FAIL

    @Schema(description = "The current progress of a test")
    enum class TestProgress {
      INCOMPLETE, COMPLETE, SUCCESS, FAIL;
    }
  }
}
