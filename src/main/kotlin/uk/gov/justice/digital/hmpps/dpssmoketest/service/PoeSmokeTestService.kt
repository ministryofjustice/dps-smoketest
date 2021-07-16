package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus

@Service
class PoeSmokeTestService(
  private val prisonService: PrisonService
) {
  fun runSmokeTest(testProfile: PoeTestParameters): Flux<TestStatus> {
    return Flux.concat(
      Flux.just(TestStatus("Will release prisoner ${testProfile.nomsNumber}", TestStatus.TestProgress.INCOMPLETE)),
      // Trigger test by releasing prisoner
      Flux.from(prisonService.triggerPoeReleaseTest(testProfile.nomsNumber)),
      // Check for hmpps domain release event
      Flux.from(prisonService.waitForEventToBeProduced(testProfile.nomsNumber)),

      // Trigger test by recalling prisoner
      // Flux.from(prisonService.triggerPoeRecallTest(it.nomsNumber)),
      // Check for hmpps domain recall event
      // prisonService.waitForEventToBeProduced(testProfile.nomsNumber),
    ).takeUntil(TestStatus::hasResult)
  }
}
