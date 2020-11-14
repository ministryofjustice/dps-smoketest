package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.PtpuTestParameters
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult

data class PtpuTestInputs(val crn: String, val nomsNumber: String, val bookingNumber: String, val prisonCode: String, val testResult: TestResult)

@Service
class SmokeTestService(
  private val prisonService: PrisonService,
  private val communityService: CommunityService,
) {

  fun runSmokeTestPtpu(testProfile: PtpuTestParameters): Flux<TestResult> {
    return Flux.from(prisonService.getTestInputs(testProfile.nomsNumber, testProfile.crn))
      .flatMap {
        Flux.concat(
          Flux.just(it.testResult),
          Flux.from(communityService.resetTestData(it.crn)),
          Flux.from(prisonService.triggerTest(it.nomsNumber)),
          communityService.waitForTestToComplete(it.nomsNumber, it.bookingNumber),
          Flux.from(
            communityService.assertTestResult(it.nomsNumber, it.bookingNumber, it.prisonCode)
          )
        ).takeUntil(TestResult::hasResult)
      }
  }
}
