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
  private val queueService: QueueService

) {
  fun runSmokeTest(testProfile: PoeTestParameters): Flux<TestStatus> {

    // Remove purge call to test circle build
    // queueService.purgeQueue()

    return Flux.concat(
      Flux.just(TestStatus("Will release prisoner ${testProfile.nomsNumber}", INCOMPLETE)),
      Flux.from(prisonService.triggerPoeReleaseTest(testProfile.nomsNumber)),
      Flux.from(
        queueService.waitForEventToBeProduced(
          "prison-offender-events.prisoner.released", testProfile.nomsNumber,
          COMPLETE
        )
      ),
      Flux.just(TestStatus("Will recall prisoner ${testProfile.nomsNumber}", INCOMPLETE)),
      Flux.from(prisonService.triggerPoeRecallTest(testProfile.nomsNumber)),
      Flux.from(queueService.waitForEventToBeProduced("prison-offender-events.prisoner.received", testProfile.nomsNumber, SUCCESS)),
    ).takeUntil(TestStatus::hasResult)

    // TODO Question->shall we delete the queue?
  }
}
