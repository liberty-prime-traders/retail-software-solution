package me.ezra_home.retail_software_solution.cucumber.steps.platform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.support.KafkaConsumerTestSupport
import me.ezra_home.retail_software_solution.cucumber.support.context.KafkaContext
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.support.TestConstants
import org.awaitility.Awaitility.await
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KafkaConsumerSteps(
  private val responseContext: ResponseContext,
  private val objectMapper: ObjectMapper,
  private val kafkaConsumerTestSupport: KafkaConsumerTestSupport,
  private val kafkaContext: KafkaContext
) {

  @Then("a catalog event should be published for table {string}")
  fun verifyCatalogEventPublished(tableName: String) {
    val consumer = assertNotNull(
      kafkaConsumerTestSupport.catalogEventsConsumer,
      "Kafka consumer not initialized — add @publishes-to-kafka tag to the scenario"
    )

    var matchedEvent: JsonNode? = null

    await().alias("No catalog event received for table $tableName")
      .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_EVENT_MS))
      .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
      .until {
        val records = consumer.poll(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
        for (record in records) {
          val payload = objectMapper.readTree(record.value())
          if (payload.path("tableName").asText() == tableName) {
            matchedEvent = payload
            return@until true
          }
        }
        false
      }

    kafkaContext.lastCatalogEvent = matchedEvent
  }

  @Then("the catalog event should reference the created resource")
  fun verifyCatalogEventEntityMatchesResponse() {
    val event = assertNotNull(kafkaContext.lastCatalogEvent, "No catalog event captured")
    val createdResourceId = assertNotNull(
      responseContext.idFromResponse(),
      "No created resource id found in last response"
    )
    assertEquals(createdResourceId.toString(), event.path("entityId").asText())
  }
}
