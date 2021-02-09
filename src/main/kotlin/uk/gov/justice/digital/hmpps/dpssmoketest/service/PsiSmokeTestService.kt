package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import kotlin.random.Random.Default.nextInt

@Service
class PsiSmokeTestService(
  private val probationSearchService: ProbationSearchService,
  private val communityService: CommunityService,
) {

  fun runSmokeTest(testProfile: PsiTestParameters): Flux<TestStatus> {
    val (firstName, surname) = generateRandomNames()
    return Flux.concat(
      Flux.from(communityService.checkOffenderExists(testProfile.crn)),
      Flux.just(TestStatus("Will update name to $firstName $surname")),
      Flux.from(communityService.setOffenderDetailsTestData(testProfile.crn, firstName, surname)),
      probationSearchService.waitForOffenderToBeFound(testProfile.crn, firstName, surname),
      Flux.from(
        probationSearchService.assertTestResult(testProfile.crn, firstName, surname)
      )

    ).takeUntil(TestStatus::hasResult)
  }

  fun cleanup(testProfile: PsiTestParameters): Mono<TestStatus> =
    communityService.setOffenderDetailsTestData(testProfile.crn, "PSI", "Smoketest")
}
private fun generateRandomNames(): Pair<String, String> {
  return randomString() to randomString()
}

private fun randomString(): String {
  val charPool: List<Char> = ('a'..'z') + ('A'..'Z')

  return (1..10)
    .map { nextInt(0, charPool.size) }
    .map(charPool::get)
    .joinToString("")
}
