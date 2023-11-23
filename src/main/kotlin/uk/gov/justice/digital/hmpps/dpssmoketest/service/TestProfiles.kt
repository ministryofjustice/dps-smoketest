package uk.gov.justice.digital.hmpps.dpssmoketest.service

// Probation search indexer
data class PsiTestParameters(val crn: String)

enum class PsiTestProfiles(val profile: PsiTestParameters) {
  PSI_T3(PsiTestParameters("X379864")),
}

// Prison offender events
data class PoeTestParameters(val nomsNumber: String)

enum class PoeTestProfiles(val profile: PoeTestParameters) {
  POE_T3(PoeTestParameters("A7851DY")),
}
