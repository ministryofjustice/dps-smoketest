package uk.gov.justice.digital.hmpps.dpssmoketest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class DpsSmoketest

fun main(args: Array<String>) {
  runApplication<DpsSmoketest>(*args)
}
