package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult

@Service
class SmokeTestService(
  private val prisonService: PrisonService,
  private val communityService: CommunityService,
) {

  fun runSmokeTest(): Flux<TestResult> {

    return Flux.concat(
      Flux.from(communityService.resetTestData("X360040")),
      Flux.from(prisonService.triggerTest("A7742DY")),
      communityService.waitForTestToComplete("A7742DY", "38479A"),
      Flux.from(communityService.assertTestResult("A7742DY", "38479A"))
    )
      .takeUntil { it.testStatus.hasResult() }
  }
}
