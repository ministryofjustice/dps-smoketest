package uk.gov.justice.digital.hmpps.dpssmoketest.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${prisonapi.endpoint.url}") private val prisonapiRootUri: String,
  @Value("\${prisonersearch.endpoint.url}") private val prisonerSearchUri: String,
  @Value("\${api.timeout:10s}") val timeout: Duration,
) {
  @Bean
  fun prisonApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient =
    builder.authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = "prison-api",
      url = prisonapiRootUri,
      timeout = timeout,
    )

  @Bean
  fun prisonerSearchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient =
    builder.authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = "prisoner-search",
      url = prisonerSearchUri,
      timeout = timeout,
    )
}
