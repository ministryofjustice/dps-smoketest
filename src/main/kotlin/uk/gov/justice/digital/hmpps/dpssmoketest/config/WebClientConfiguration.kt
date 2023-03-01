package uk.gov.justice.digital.hmpps.dpssmoketest.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${community.endpoint.url}") private val communityRootUri: String,
  @Value("\${prisonapi.endpoint.url}") private val prisonapiRootUri: String,
  @Value("\${oauth.endpoint.url}") private val oauthRootUri: String,
  @Value("\${probationoffendersearch.endpoint.url}") private val probationOffenderSearchUri: String,
) {

  @Bean
  fun communityApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("community-api")

    return WebClient.builder()
      .baseUrl(communityRootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prison-api")

    return WebClient.builder()
      .baseUrl(prisonapiRootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun probationOffenderSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("probation-offender-search")

    return WebClient.builder()
      .baseUrl(probationOffenderSearchUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun communityApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(communityRootUri).build()
  }

  @Bean
  fun prisonApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonapiRootUri).build()
  }

  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(oauthRootUri).build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
