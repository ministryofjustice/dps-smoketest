package uk.gov.justice.digital.hmpps.dpssmoketest.service.ptpu

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus

data class PtpuTestInputs(val crn: String, val nomsNumber: String, val bookingNumber: String, val prisonCode: String, val testStatus: TestStatus)

@Service
class SmokeTestServicePtpu(
  private val prisonService: PrisonService,
  private val communityService: CommunityService,
) {

  fun runSmokeTest(testProfile: PtpuTestParameters): Flux<TestStatus> {
    return Flux.from(prisonService.getTestInputs(testProfile.nomsNumber, testProfile.crn))
      .flatMap {
        Flux.concat(
          Flux.just(it.testStatus),
          Flux.from(communityService.resetTestData(it.crn)),
          Flux.from(prisonService.triggerTest(it.nomsNumber)),
          communityService.waitForTestToComplete(it.nomsNumber, it.bookingNumber),
          Flux.from(
            communityService.assertTestResult(it.nomsNumber, it.bookingNumber, it.prisonCode)
          )
        ).takeUntil(TestStatus::hasResult)
      }
  }
}
