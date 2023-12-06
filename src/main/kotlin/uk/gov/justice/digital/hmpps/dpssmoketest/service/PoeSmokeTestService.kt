package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.INCOMPLETE
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.SUCCESS

@Service
class PoeSmokeTestService(
  private val prisonService: PrisonService,
  private val queueService: QueueService,

) {
  fun runSmokeTest(testProfile: PoeTestParameters): Flux<TestStatus> {
    queueService.purgeQueue()

    return Flux.concat(
      Flux.just(TestStatus("Will release prisoner ${testProfile.nomsNumber}", INCOMPLETE)),
      prisonService.triggerPoeReleaseTest(testProfile.nomsNumber),
      queueService.waitForEventToBeProduced(
        "prison-offender-events.prisoner.released",
        testProfile.nomsNumber,
        COMPLETE,
      ),
      Flux.just(TestStatus("Will recall prisoner ${testProfile.nomsNumber}", INCOMPLETE)),
      prisonService.triggerPoeRecallTest(testProfile.nomsNumber),
      queueService.waitForEventToBeProduced("prison-offender-events.prisoner.received", testProfile.nomsNumber, SUCCESS),
    ).takeUntil(TestStatus::hasResult)
  }
}
