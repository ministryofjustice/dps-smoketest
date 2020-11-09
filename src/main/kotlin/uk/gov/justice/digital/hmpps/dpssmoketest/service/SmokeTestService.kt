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

  fun runSmokeTest(testMode: TestMode): Flux<TestResult> {
    val probationDataResetResult = communityService.resetTestData("X360040");
    if (probationDataResetResult.outcome == false) return Flux.just(probationDataResetResult)

    val triggerTestResult = prisonService.triggerTest("A7742DY")
    if (triggerTestResult.outcome == false) return Flux.fromIterable(listOf(probationDataResetResult, triggerTestResult))

    return Flux.concat(
        Flux.just(probationDataResetResult),
        Flux.just(triggerTestResult),
        communityService.checkTestResults(testMode),
        Flux.just(communityService.checkTestResult(testMode, lastTest = true).testResult)
    )
  }

}
