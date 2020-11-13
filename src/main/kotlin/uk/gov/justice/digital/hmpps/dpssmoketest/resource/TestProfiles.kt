package uk.gov.justice.digital.hmpps.dpssmoketest.resource

data class PtpuTestProfile(val crn: String, val nomsNumber: String, val bookingNumber: String, val prisonCode: String)

enum class PtpuTestProfiles(val profile: PtpuTestProfile) {
  PTPU_T3(PtpuTestProfile("X360040", "A7742DY", "38479A", "MDI"))
}
