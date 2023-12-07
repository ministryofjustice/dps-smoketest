package uk.gov.justice.digital.hmpps.dpssmoketest.service

// Prisoner search indexer
data class PsiTestParameters(val nomsNumber: String)

enum class PsiTestProfiles(val profile: PsiTestParameters) {
  PSI_T3(PsiTestParameters("A7940DY")),
  PS_T3(PsiTestParameters("A8010DY")),
  PSI_LOCAL(PsiTestParameters("A1183SH")),
}

// Prison offender events
data class PoeTestParameters(val nomsNumber: String)

enum class PoeTestProfiles(val profile: PoeTestParameters) {
  POE_T3(PoeTestParameters("A7851DY")),
}
