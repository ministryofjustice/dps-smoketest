package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.PtpuTestProfile
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult

@Service
class SmokeTestService(
  private val prisonService: PrisonService,
  private val communityService: CommunityService,
) {

  fun runSmokeTestPtpu(testProfile: PtpuTestProfile): Flux<TestResult> =
    Flux.concat(
      Flux.from(communityService.resetTestData(testProfile.crn)),
      Flux.from(prisonService.triggerTest(testProfile.nomsNumber)),
      communityService.waitForTestToComplete(testProfile.nomsNumber, testProfile.bookingNumber),
      Flux.from(
        communityService.assertTestResult(testProfile.nomsNumber, testProfile.bookingNumber, testProfile.prisonCode)
      )
    )
      .takeUntil { it.testStatus.hasResult() }
}
