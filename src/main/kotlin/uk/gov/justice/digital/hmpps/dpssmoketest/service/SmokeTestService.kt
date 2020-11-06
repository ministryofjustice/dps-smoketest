package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestMode
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult

@Service
class SmokeTestService(
    private val prisonService: PrisonService,
    private val communityService: CommunityService,
) {

  fun runSmokeTest(testMode: TestMode): Flux<TestResult> = Flux.concat(
      Flux.just(communityService.resetTestData()),
      Flux.just(prisonService.triggerTest()),
      communityService.checkTestResults(testMode),
      Flux.just(communityService.checkTestResult(testMode, lastTest = true).testResult)
  )

}
