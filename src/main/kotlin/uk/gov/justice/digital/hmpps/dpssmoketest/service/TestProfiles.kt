package uk.gov.justice.digital.hmpps.dpssmoketest.service

data class PtpuTestParameters(val crn: String, val nomsNumber: String)

enum class PtpuTestProfiles(val profile: PtpuTestParameters) {
  PTPU_T3(PtpuTestParameters("X360040", "A7742DY"))
}
