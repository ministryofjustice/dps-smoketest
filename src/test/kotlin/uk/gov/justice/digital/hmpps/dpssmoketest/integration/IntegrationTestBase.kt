package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import uk.gov.justice.digital.hmpps.dpssmoketest.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.CommunityApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.ProbationOffenderSearchExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.service.QueueService
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(
  OAuthExtension::class,
  CommunityApiExtension::class,
  PrisonApiExtension::class,
  ProbationOffenderSearchExtension::class
)
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @SpyBean
  protected lateinit var queueService: QueueService

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @SpyBean
  lateinit var hmppsQueueService: HmppsQueueService

  init {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }
  internal val hmppsEventQueue by lazy { hmppsQueueService.findByQueueId("hmppseventqueue") as HmppsQueue }
  internal val hmppsEventQueueUrl by lazy { hmppsEventQueue.queueUrl }
  internal val hmppsEventQueueName by lazy { hmppsEventQueue.queueName }
  internal val hmppsEventQueueSqsClient by lazy { hmppsEventQueue.sqsClient }
}

internal fun SqsAsyncClient.countMessagesOnQueue(queueUrl: String): Int =
  this.getQueueAttributes(GetQueueAttributesRequest.builder().queueUrl(queueUrl).attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES).build())
    .let { it.get().attributes()[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES]?.toInt() ?: 0 }
