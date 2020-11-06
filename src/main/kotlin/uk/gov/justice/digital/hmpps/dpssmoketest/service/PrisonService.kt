package uk.gov.justice.digital.hmpps.dpssmoketest.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestResult

@Service
class PrisonService {

  fun triggerTest(): TestResult {
    Thread.sleep(100)
    return TestResult("Test triggered")
  }

}