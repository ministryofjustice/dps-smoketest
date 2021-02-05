package uk.gov.justice.digital.hmpps.dpssmoketest.service

// Prion to probation update
data class PtpuTestParameters(val crn: String, val nomsNumber: String)

enum class PtpuTestProfiles(val profile: PtpuTestParameters) {
  PTPU_T3(PtpuTestParameters("X360040", "A7742DY"))
}

// Probation search indexer
data class PsiTestParameters(val crn: String)

enum class PsiTestProfiles(val profile: PsiTestParameters) {
  PSI_T3(PsiTestParameters("X360040"))
}
