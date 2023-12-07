package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import kotlin.random.Random.Default.nextInt

@Service
class PsiSmokeTestService(
  private val prisonerSearchService: PrisonerSearchService,
  private val prisonService: PrisonService,
) {

  fun runSmokeTest(testProfile: PsiTestParameters): Flux<TestStatus> =
    generateRandomNames().let { (firstName, lastName) ->
      Flux.concat(
        prisonerSearchService.checkOffenderExists(testProfile.nomsNumber),
        Flux.just(TestStatus("Will update name to $firstName $lastName")),
        prisonService.setOffenderDetailsTestData(testProfile.nomsNumber, firstName, lastName),
        prisonerSearchService.waitForOffenderToBeFound(testProfile.nomsNumber, firstName, lastName),
        prisonerSearchService.assertTestResult(testProfile.nomsNumber, firstName, lastName),
      ).takeUntil(TestStatus::hasResult)
    }

  fun cleanup(testProfile: PsiTestParameters): Mono<TestStatus> =
    prisonService.setOffenderDetailsTestData(testProfile.nomsNumber, "PSI", "SMOKETEST")
}
private fun generateRandomNames(): Pair<String, String> = randomUpperString() to randomUpperString()

private fun randomUpperString(): String {
  val charPool: List<Char> = ('A'..'Z').toList()

  return (1..10)
    .map { nextInt(0, charPool.size) }
    .map(charPool::get)
    .joinToString("")
}
