package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.service.ptpu.CommunityService
import java.time.Duration

data class PtpuTestInputs(val crn: String, val nomsNumber: String, val bookingNumber: String, val prisonCode: String, val testStatus: TestStatus)

@Service
class SmokeTestService(
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
          waitForUpdates(),
          Flux.from(
            communityService.assertTestResult(it.nomsNumber, it.bookingNumber, it.prisonCode)
          )
        ).takeUntil(TestStatus::hasResult)
      }
  }

  private fun waitForUpdates(): Publisher<out TestStatus> =
    Flux.interval(Duration.ofMillis(1000))
      .take(1)
      .flatMap { Mono.just(TestStatus("Waiting for final update", COMPLETE)) }
}
