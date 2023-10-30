package uk.gov.justice.digital.hmpps.dpssmoketest.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import kotlin.apply as kotlinApply

@Configuration
class WebClientConfiguration(
  @Value("\${community.endpoint.url}") private val communityRootUri: String,
  @Value("\${prisonapi.endpoint.url}") private val prisonapiRootUri: String,
  @Value("\${probationoffendersearch.endpoint.url}") private val probationOffenderSearchUri: String,
  @Value("\${api.timeout:10s}") val timeout: Duration,
) {

  @Bean
  fun communityApiWebClient(authorizedClientManager: ReactiveOAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    authorisedWebClient(authorizedClientManager, builder, registrationId = "community-api", url = communityRootUri)

  @Bean
  fun prisonApiWebClient(authorizedClientManager: ReactiveOAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    authorisedWebClient(authorizedClientManager, builder, registrationId = "prison-api", url = prisonapiRootUri)

  @Bean
  fun probationOffenderSearchWebClient(authorizedClientManager: ReactiveOAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    authorisedWebClient(authorizedClientManager, builder, registrationId = "probation-offender-search", url = probationOffenderSearchUri)

  private fun authorisedWebClient(
    authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    registrationId: String,
    url: String,
  ): WebClient {
    val oauth2Client = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager).kotlinApply {
      setDefaultClientRegistrationId(registrationId)
    }

    return builder.baseUrl(url)
      .clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(timeout)))
      .filter(oauth2Client)
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ReactiveClientRegistrationRepository,
    oAuth2AuthorizedClientService: ReactiveOAuth2AuthorizedClientService,
  ): ReactiveOAuth2AuthorizedClientManager {
    val authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    return AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    ).kotlinApply { setAuthorizedClientProvider(authorizedClientProvider) }
  }
}
