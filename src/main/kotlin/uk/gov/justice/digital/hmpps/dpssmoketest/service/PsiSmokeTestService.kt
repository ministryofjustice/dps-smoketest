package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.COMPLETE

data class PsiTestInputs(val crn: String, val testStatus: TestStatus)

@Service
class PsiSmokeTestService(
  private val probationSearchService: ProbationSearchService,
  private val communityService: CommunityService,
) {

  fun runSmokeTest(testProfile: PsiTestParameters): Flux<TestStatus> {
    return Flux.just(TestStatus("All done", COMPLETE))
  }
}
