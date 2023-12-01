package uk.gov.justice.digital.hmpps.dpssmoketest.service

// Prisoner search indexer
data class PsiTestParameters(val nomsNumber: String)

enum class PsiTestProfiles(val profile: PsiTestParameters) {
  PSI_T3(PsiTestParameters("A7940DY")),
}

// Prison offender events
data class PoeTestParameters(val nomsNumber: String)

enum class PoeTestProfiles(val profile: PoeTestParameters) {
  POE_T3(PoeTestParameters("A7851DY")),
}
