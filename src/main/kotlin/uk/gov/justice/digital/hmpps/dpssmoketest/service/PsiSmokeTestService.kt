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
        Flux.from(prisonerSearchService.checkOffenderExists(testProfile.nomsNumber)),
        Flux.just(TestStatus("Will update name to $firstName $lastName")),
        Flux.from(prisonService.setOffenderDetailsTestData(testProfile.nomsNumber, firstName, lastName)),
        prisonerSearchService.waitForOffenderToBeFound(testProfile.nomsNumber, firstName, lastName),
        Flux.from(prisonerSearchService.assertTestResult(testProfile.nomsNumber, firstName, lastName)),
      ).takeUntil(TestStatus::hasResult)
    }

  fun cleanup(testProfile: PsiTestParameters): Mono<TestStatus> =
    prisonService.setOffenderDetailsTestData(testProfile.nomsNumber, "PSI", "Smoketest")
}
private fun generateRandomNames(): Pair<String, String> = randomString() to randomString()

private fun randomString(): String {
  val charPool: List<Char> = ('a'..'z') + ('A'..'Z')

  return (1..10)
    .map { nextInt(0, charPool.size) }
    .map(charPool::get)
    .joinToString("")
}
