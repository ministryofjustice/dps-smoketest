package uk.gov.justice.digital.hmpps.dpssmoketest.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import kotlin.apply as kotlinApply

@Configuration
class WebClientConfiguration(
  @Value("\${prisonapi.endpoint.url}") private val prisonapiRootUri: String,
  @Value("\${prisonersearch.endpoint.url}") private val prisonerSearchUri: String,
  @Value("\${api.timeout:10s}") val timeout: Duration,
) {
  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    builder.authorisedWebClient(authorizedClientManager, registrationId = "prison-api", url = prisonapiRootUri, timeout)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    builder.authorisedWebClient(authorizedClientManager, registrationId = "prisoner-search", url = prisonerSearchUri, timeout)

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build().let {
    AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    ).kotlinApply { setAuthorizedClientProvider(it) }
  }
}

fun WebClient.Builder.authorisedWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, registrationId: String, url: String, timeout: Duration): WebClient {
  val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager).kotlinApply {
    setDefaultClientRegistrationId(registrationId)
  }

  return baseUrl(url)
    .clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(timeout)))
    .filter(oauth2Client)
    .build()
}
