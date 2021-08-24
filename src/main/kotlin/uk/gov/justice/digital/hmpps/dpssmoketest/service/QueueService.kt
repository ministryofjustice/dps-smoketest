package uk.gov.justice.digital.hmpps.dpssmoketest.service

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.kotlin.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress
import uk.gov.justice.digital.hmpps.dpssmoketest.resource.SmokeTestResource.TestStatus.TestProgress.FAIL
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.Duration

@Service
class QueueService(
  @Value("\${test.maxLengthSeconds}") private val testMaxLengthSeconds: Long,
  @Value("\${test.resultPollMs}") private val testResultPollMs: Long,
  private val objectMapper: ObjectMapper,
  private val hmppsQueueService: HmppsQueueService
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  protected val hmppsEventQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventqueue")
      ?: throw MissingQueueException("HmppsQueue hmppseventqueue not found")
  }
  protected val hmppsEventQueueUrl by lazy { hmppsEventQueue.queueUrl }
  protected val hmppsEventQueueSqsClient by lazy { hmppsEventQueue.sqsClient }

  fun waitForEventToBeProduced(eventType: String, nomsNumber: String, finalStatus: TestProgress): Flux<TestStatus> =
    Flux.interval(Duration.ofMillis(testResultPollMs))
      .take(Duration.ofSeconds(testMaxLengthSeconds))
      .flatMap { checkEventProduced(eventType, nomsNumber, finalStatus) }
      .takeUntil(TestStatus::testComplete)

  fun checkEventProduced(eventType: String, nomsNumber: String, finalStatus: TestProgress): Mono<TestStatus> {

    fun failOnError(exception: Throwable): Mono<out TestStatus> =
      Mono.just(
        TestStatus("Check $eventType event produced $nomsNumber failed due to ${exception.message}", FAIL)
      )

    return Mono.just(checkForEvent(eventType, nomsNumber))
      .map {
        if (it)
          TestStatus("Test for offender $nomsNumber $eventType event finished successfully", finalStatus)
        else
          TestStatus("Still waiting for offender $nomsNumber $eventType event")
      }
      .onErrorResume(::failOnError)
  }

  fun checkForEvent(eventTypeRequired: String, nomsNumber: String): Boolean {
    var eventFound = false
    while (hmppsEventQueueSqsClient.countMessagesOnQueue()> 0 && !eventFound) {
      hmppsEventQueueSqsClient.receiveMessage(
        ReceiveMessageRequest(hmppsEventQueueUrl)
          .withMaxNumberOfMessages(1)
      ).messages.firstOrNull()
        ?.also { msg ->
          val (message, messageId, messageAttributes) = objectMapper.readValue(msg.body, HMPPSMessage::class.java)
          val eventType = messageAttributes.eventType.Value
          val hmppsDomainEvent = objectMapper.readValue(message, HMPPSDomainEvent::class.java)

          log.info("Received message $message $messageId type $eventType")
          hmppsEventQueueSqsClient.deleteMessage(DeleteMessageRequest(hmppsEventQueueUrl, msg.receiptHandle))
          if (eventTypeRequired == eventType && hmppsDomainEvent.additionalInformation.nomsNumber == nomsNumber) eventFound = true
        }
    }
    return eventFound
  }

  fun purgeQueue() {
    hmppsEventQueueSqsClient.purgeQueue(PurgeQueueRequest(hmppsEventQueueUrl))
    await.until { hmppsEventQueueSqsClient.countMessagesOnQueue() == 0 }
  }

  internal fun AmazonSQS.countMessagesOnQueue(): Int =
    this.getQueueAttributes(hmppsEventQueueUrl, listOf("ApproximateNumberOfMessages"))
      .let { it.attributes["ApproximateNumberOfMessages"]?.toInt() ?: 0 }
}

data class AdditionalInformation(val nomsNumber: String, val details: String, val reason: String, val prisonId: String)
data class HMPPSDomainEvent(val additionalInformation: AdditionalInformation)

data class HMPPSEventType(val Value: String, val Type: String)
data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
data class HMPPSMessage(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: HMPPSMessageAttributes
)
