package uk.gov.justice.digital.hmpps.dpssmoketest.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ReactorClientHttpRequestFactory
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration
import kotlin.apply as kotlinApply

private const val DEFAULT_TIMEOUT_SECONDS: Long = 30

@Configuration
class WebClientConfiguration(
  @Value("\${prisonapi.endpoint.url}") private val prisonapiRootUri: String,
  @Value("\${prisonersearch.endpoint.url}") private val prisonerSearchUri: String,
  @Value("\${api.timeout:10s}") val timeout: Duration,
) {
  @Bean
  fun webClientBuilder(): WebClient.Builder = WebClient.builder()

  @Bean
  fun prisonApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = smokeTestUserAwareTokenRequestOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    ),
    registrationId = "prison-api",
    url = prisonapiRootUri,
    timeout = timeout,
  )

  @Bean
  fun prisonerSearchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "prisoner-search",
    url = prisonerSearchUri,
    timeout = timeout,
  )
}

// Copied from userAwareTokenRequestOAuth2AuthorizedClientManager with update specific to smoke test user
fun smokeTestUserAwareTokenRequestOAuth2AuthorizedClientManager(
  clientRegistrationRepository: ClientRegistrationRepository,
  oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
  clientCredentialsTokenRequestTimeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS),
): OAuth2AuthorizedClientManager {
  val usernameAwareRestClientClientCredentialsTokenResponseClient =
    createAccessTokenResponseClient(clientCredentialsTokenRequestTimeout).kotlinApply {
      setParametersCustomizer { it.add("username", "SMOKE_TEST_USER") }
    }

  val oAuth2AuthorizedClientProvider =
    OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials {
      it.accessTokenResponseClient(usernameAwareRestClientClientCredentialsTokenResponseClient)
    }.build()

  return AuthorizedClientServiceOAuth2AuthorizedClientManager(
    clientRegistrationRepository,
    oAuth2AuthorizedClientService,
  ).kotlinApply {
    setAuthorizedClientProvider(oAuth2AuthorizedClientProvider)
  }
}
fun createAccessTokenResponseClient(clientCredentialsClientTimeout: Duration): RestClientClientCredentialsTokenResponseClient = RestClientClientCredentialsTokenResponseClient().kotlinApply {
  val requestFactory = ReactorClientHttpRequestFactory().kotlinApply {
    setReadTimeout(clientCredentialsClientTimeout)
  }

  // code duplicated from AbstractRestClientOAuth2AccessTokenResponseClient.restClient
  // so that we can set our requestFactory
  val restClient = RestClient.builder()
    .configureMessageConverters { configurer ->
      configurer.addCustomConverter(FormHttpMessageConverter())
      configurer.addCustomConverter(OAuth2AccessTokenResponseHttpMessageConverter())
    }
    .defaultStatusHandler(OAuth2ErrorResponseErrorHandler())
    .requestFactory(requestFactory)
    .build()

  setRestClient(restClient)
}
