package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource
import uk.gov.justice.digital.hmpps.dpssmoketest.service.ptpu.CommunityService
import uk.gov.justice.digital.hmpps.dpssmoketest.service.ptpu.PrisonService
import uk.gov.justice.digital.hmpps.dpssmoketest.service.ptpu.PtpuTestParameters

data class PtpuTestInputs(val crn: String, val nomsNumber: String, val bookingNumber: String, val prisonCode: String, val testStatus: SmokeTestResource.TestStatus)

@Service
class PtpuSmokeTestService(
  private val prisonService: PrisonService,
  private val communityService: CommunityService,
) {

  fun runSmokeTest(testProfile: PtpuTestParameters): Flux<SmokeTestResource.TestStatus> {
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
        ).takeUntil(SmokeTestResource.TestStatus::hasResult)
      }
  }
}