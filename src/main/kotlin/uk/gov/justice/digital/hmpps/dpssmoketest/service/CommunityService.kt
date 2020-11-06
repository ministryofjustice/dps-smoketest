package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestMode
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestMode.SUCCEED
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestMode.TIMEOUT
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult
import java.time.Duration

data class TestStatus(val testComplete: Boolean, val testResult: TestResult)

@Service
class CommunityService(
    @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
    @Value("\${test.resultPollMs}") private val testResultPollMs: Long
) {

  /*
   * The number of times to poll before completing a SUCCEED or FAIL test
   * e.g. in SmokeTestIntegrationTest this is (10*1000)/(2*1000)=5 polls (taking 5 seconds)
   * e.g. in a Circle build this is (600*1000)/(2*10000)=30 polls (taking 300 seconds)
   */
  val maxTestPollCount = (testMaxLengthSeconds*1000)/(2*testResultPollMs)

  fun resetTestData(): TestResult {
    Thread.sleep(100)
    return TestResult("Reset Community test data")
  }

  fun checkTestResults(testMode: TestMode): Flux<TestResult> {
    var testPollCount = 0
    return Flux.interval(Duration.ofMillis(testResultPollMs))
        .take(Duration.ofSeconds(testMaxLengthSeconds))
        .map { checkTestResult(testMode, testPollCount++) }
        .takeWhile { it.testComplete.not() }
        .map { it.testResult }
  }

  fun checkTestResult(testMode: TestMode, testPollCount: Int = 0, lastTest: Boolean = false): TestStatus {
    Thread.sleep(100)
    if (testMode == TIMEOUT)
      return TestStatus(testComplete = false, TestResult("Test still running (testMode=TIMEOUT)"))

    if (lastTest.not() && testPollCount < maxTestPollCount)
      return TestStatus(testComplete = false, TestResult("Test still running (testMode=$testMode)"))

    return if (testMode == SUCCEED)
      TestStatus(testComplete = true, TestResult("Test has completed successfully", true))
    else
      TestStatus(testComplete = true, TestResult("Test has failed", true))
  }

}