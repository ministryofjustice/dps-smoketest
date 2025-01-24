@file:Suppress("PropertyName")

package uk.gov.justice.digital.hmpps.dpssmoketest.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.awaitility.kotlin.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
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
  private val hmppsQueueService: HmppsQueueService,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  protected val hmppsEventQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventqueue")
      ?: throw MissingQueueException("HmppsQueue hmppseventqueue not found")
  }
  protected val hmppsEventQueueUrl by lazy { hmppsEventQueue.queueUrl }
  protected val hmppsEventQueueSqsClient by lazy { hmppsEventQueue.sqsClient }

  fun waitForEventToBeProduced(eventType: String, nomsNumber: String, finalStatus: TestProgress): Flux<TestStatus> = Flux.interval(Duration.ofMillis(testResultPollMs))
    .take(Duration.ofSeconds(testMaxLengthSeconds))
    .flatMap { checkEventProduced(eventType, nomsNumber, finalStatus) }
    .takeUntil(TestStatus::testComplete)

  fun checkEventProduced(eventType: String, nomsNumber: String, finalStatus: TestProgress): Mono<TestStatus> {
    fun failOnError(exception: Throwable): Mono<out TestStatus> = Mono.just(
      TestStatus("Check $eventType event produced $nomsNumber failed due to ${exception.message}", FAIL),
    )

    return Mono.just(checkForEvent(eventType, nomsNumber))
      .map {
        if (it) {
          TestStatus("Test for offender $nomsNumber $eventType event finished successfully", finalStatus)
        } else {
          TestStatus("Still waiting for offender $nomsNumber $eventType event")
        }.also { status -> log.info("Result is $status - waiting total of $testMaxLengthSeconds") }
      }
      .onErrorResume(::failOnError)
  }

  fun checkForEvent(eventTypeRequired: String, nomsNumber: String): Boolean {
    var eventFound = false
    while (hmppsEventQueueSqsClient.countMessagesOnQueue(hmppsEventQueueUrl) > 0 && !eventFound) {
      hmppsEventQueueSqsClient.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(hmppsEventQueueUrl).maxNumberOfMessages(1).build(),
      ).get().messages().firstOrNull()
        ?.also { msg ->
          val (message, messageId, messageAttributes) = objectMapper.readValue(msg.body(), HMPPSMessage::class.java)
          val eventType = messageAttributes.eventType.Value
          val hmppsDomainEvent = objectMapper.readValue(message, HMPPSDomainEvent::class.java)

          log.info("Received message $message $messageId type $eventType")
          hmppsEventQueueSqsClient.deleteMessage(
            DeleteMessageRequest.builder().queueUrl(hmppsEventQueueUrl).receiptHandle(msg.receiptHandle()).build(),
          ).get()
          if (eventTypeRequired == eventType && hmppsDomainEvent.additionalInformation.nomsNumber == nomsNumber) eventFound = true
        }
    }
    return eventFound
  }

  fun purgeQueue() {
    hmppsEventQueueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsEventQueueUrl).build()).get()
    await.until { hmppsEventQueueSqsClient.countMessagesOnQueue(hmppsEventQueueUrl) == 0 }
  }

  internal fun SqsAsyncClient.countMessagesOnQueue(queueUrl: String): Int = this.getQueueAttributes(GetQueueAttributesRequest.builder().queueUrl(queueUrl).attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES).build())
    .let { it.get().attributes()[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES]?.toInt() ?: 0 }
}

data class AdditionalInformation(val nomsNumber: String, val details: String?, val reason: String, val prisonId: String)
data class HMPPSDomainEvent(val additionalInformation: AdditionalInformation)

data class HMPPSEventType(val Value: String, val Type: String)
data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
data class HMPPSMessage(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: HMPPSMessageAttributes,
)
