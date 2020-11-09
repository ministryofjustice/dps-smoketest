package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
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
    @Value("\${test.resultPollMs}") private val testResultPollMs: Long,
    @Qualifier("communityApiWebClient") private val webClient: WebClient
) {

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

  /*
   * The number of times to poll before completing a SUCCEED or FAIL test
   * e.g. in SmokeTestIntegrationTest this is (2*10*1000)/(3*1000)=6 polls (taking 6 seconds)
   * e.g. in a Circle build this is (2*600*1000)/(3*10000)=40 polls (taking 400 seconds)
   */
  val maxTestPollCount = (2*testMaxLengthSeconds*1000)/(3*testResultPollMs)

  fun resetTestData(crn: String): TestResult {
    Thread.sleep(100)
    return TestResult("Reset Community test data")
  }

  fun checkTestResults(testMode: TestMode): Flux<TestResult> {
    var testPollCount = 0
    return Flux.interval(Duration.ofMillis(testResultPollMs))
        .take(Duration.ofSeconds(testMaxLengthSeconds))
        .map {checkTestResult(testMode, testPollCount++) }
        .takeWhile { it.testComplete.not() }
        .map { it.testResult }
        .doOnError { log.error("Error received polling test results", it) }
        .doOnComplete { log.info("Completed polling test results") }
        .doOnCancel { log.info("Cancelled polling test results") }
        .doOnTerminate { log.info("Terminated polling test results") }
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
      TestStatus(testComplete = true, TestResult("Test has failed", false))
  }

}