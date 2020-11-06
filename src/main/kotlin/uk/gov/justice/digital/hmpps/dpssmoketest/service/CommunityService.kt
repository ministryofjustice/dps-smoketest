package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestMode
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestMode.SUCCEED
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestMode.TIMEOUT
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import java.time.Duration

data class TestStatus(val testComplete: Boolean, val testResult: TestResult)

@Service
class CommunityService {

  fun resetTestData(): TestResult {
    Thread.sleep(100)
    return TestResult("Reset Community test data ")
  }

  fun checkTestResults(testMode: TestMode): Flux<TestResult> =
    Flux.interval(Duration.ofMillis(1000))
        .take(Duration.ofSeconds(10))
        .map { checkTestResult(testMode) }
        .takeWhile { it.testComplete.not() }
        .map { it.testResult }

  fun checkTestResult(testMode: TestMode, lastTest: Boolean = false): TestStatus {
    Thread.sleep(100)
    if (testMode == TIMEOUT)
      return TestStatus(testComplete = false, TestResult("Test still running (testMode=TIMEOUT)"))

    val randomizer = Math.random()
    if (lastTest.not() && randomizer < 0.9)
      return TestStatus(testComplete = false, TestResult("Test still running (testMode=$testMode)"))

    return if (testMode == SUCCEED)
      TestStatus(testComplete = true, TestResult("Test has completed successfully", true))
    else
      TestStatus(testComplete = true, TestResult("Test has failed", true))
  }

}