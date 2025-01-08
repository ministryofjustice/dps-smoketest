package uk.gov.justice.digital.hmpps.dpssmoketest.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.integration.wiremock.PrisonerSearchExtension
import uk.gov.justice.digital.hmpps.dpssmoketest.service.QueueService
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(
  OAuthExtension::class,
  PrisonApiExtension::class,
  PrisonerSearchExtension::class,
)
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @MockitoSpyBean
  protected lateinit var queueService: QueueService

  @Autowired
  lateinit var webTestClient: WebTestClient

  @MockitoSpyBean
  lateinit var hmppsQueueService: HmppsQueueService

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }
  internal val hmppsEventQueue by lazy { hmppsQueueService.findByQueueId("hmppseventqueue") as HmppsQueue }
  internal val hmppsEventQueueUrl by lazy { hmppsEventQueue.queueUrl }
  internal val hmppsEventQueueName by lazy { hmppsEventQueue.queueName }
  internal val hmppsEventQueueSqsClient by lazy { hmppsEventQueue.sqsClient }
}
