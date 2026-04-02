package me.ezra_home.retail_software_solution.cucumber.steps.platform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.context.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility.await
import org.springframework.beans.factory.annotation.Value
import java.time.Duration
import java.util.Properties
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class KafkaConsumerSteps(
  private val responseContext: ResponseContext,
  @param:Value("\${spring.kafka.bootstrap-servers}")
  private val bootstrapServers: String
) {

  private var kafkaConsumer: KafkaConsumer<String, String>? = null
  private var lastCatalogEvent: JsonNode? = null

  companion object {
    private val OBJECT_MAPPER = jacksonObjectMapper()
  }

  @After("@kafka-producer")
  fun closeKafkaConsumer() {
    kafkaConsumer?.close()
    kafkaConsumer = null
  }

  @Given("I am subscribed to the catalog events topic")
  fun subscribeToCatalogEvents() {
    val properties = Properties().apply {
      put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      put(ConsumerConfig.GROUP_ID_CONFIG, "cucumber-catalog-${UUID.randomUUID()}")
      put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
      put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
      put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    }

    val consumer = KafkaConsumer<String, String>(properties)
    val topic = KafkaConstants.Topics.CATALOG_EVENTS
    val partitions = consumer.partitionsFor(topic).map { TopicPartition(topic, it.partition()) }
    consumer.assign(partitions)
    consumer.poll(Duration.ofMillis(200))

    val endOffsets = consumer.endOffsets(partitions)
    partitions.forEach { consumer.seek(it, endOffsets[it] ?: 0L) }

    kafkaConsumer = consumer
  }

  @Then("a catalog event should be published for table {string}")
  fun verifyCatalogEventPublished(tableName: String) {
    val consumer = assertNotNull(kafkaConsumer, "Kafka consumer not initialized — call: I am subscribed to the catalog events topic")

    var matchedEvent: JsonNode? = null

    await().alias("No catalog event received for table $tableName")
      .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_EVENT_MS))
      .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
      .until {
        val records = consumer.poll(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
        for (record in records) {
          val payload = OBJECT_MAPPER.readTree(record.value())
          if (payload.path("tableName").asText() == tableName) {
            matchedEvent = payload
            return@until true
          }
        }
        false
      }

    lastCatalogEvent = matchedEvent
  }

  @Then("the catalog event should reference the created resource")
  fun verifyCatalogEventEntityMatchesResponse() {
    val event = assertNotNull(lastCatalogEvent, "No catalog event captured")
    val createdResourceId = assertNotNull(
      responseContext.lastResponse?.jsonPath()?.getString("id"),
      "No created resource id found in last response"
    )
    assertEquals(createdResourceId, event.path("entityId").asText())
  }
}
