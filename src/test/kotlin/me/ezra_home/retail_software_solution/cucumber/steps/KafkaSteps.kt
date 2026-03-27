package me.ezra_home.retail_software_solution.cucumber.steps

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.fixtures.locations.LocationFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants.DEFAULT_ID
import me.ezra_home.retail_software_solution.cucumber.support.withSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.organizations.business.location.LocationRepository
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.util.enums.LocationType
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import java.time.Duration
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class KafkaSteps(
    private val authContext: AuthContext,
    private val responseContext: ResponseContext,
    private val locationFixtureBuilder: LocationFixtureBuilder,
    private val locationRepository: LocationRepository,
    private val locationProductRepository: LocationProductRepository,

    @param:Value("\${spring.kafka.bootstrap-servers}")
    private val bootstrapServers: String
) {

  private var catalogSyncLocationId: UUID? = null
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

  @Given("a location exists for catalog sync")
  fun createLocationForCatalogSync() {
    val locationId = locationFixtureBuilder.create(
      LocationInsertDto(
        locationType = LocationType.SHOP,
        name = "Catalog Sync Location",
        description = "Location used in cucumber kafka consumer flow"
      )
    )
    catalogSyncLocationId = locationId
    authContext.currentLocationId = locationId
  }

  @Given("I use the catalog sync location context")
  fun useCatalogSyncLocationContext() {
    assertNotNull(catalogSyncLocationId, "Catalog sync location id was not set")
    authContext.currentLocationId = catalogSyncLocationId
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
    val consumer = kafkaConsumer
    assertNotNull(consumer, "Kafka consumer was not initialized. Use step: I am subscribed to the catalog events topic")

    val timeoutAt = System.currentTimeMillis() + TestConstants.Timeouts.KAFKA_EVENT_MS
    var matchedEvent: JsonNode? = null

    while (System.currentTimeMillis() < timeoutAt && matchedEvent == null) {
      val records = consumer.poll(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
      for (record in records) {
        val payload = OBJECT_MAPPER.readTree(record.value())
        if (payload.path("tableName").asText() == tableName) {
          matchedEvent = payload
          break
        }
      }
    }

    assertNotNull(matchedEvent, "No catalog event received for table $tableName within timeout")
    lastCatalogEvent = matchedEvent
  }

  @Then("the catalog event should reference the created resource")
  fun verifyCatalogEventEntityMatchesResponse() {
    val event = lastCatalogEvent
    assertNotNull(event, "No catalog event captured")

    val createdResourceId = responseContext.lastResponse?.jsonPath()?.getString("id")
    assertNotNull(createdResourceId, "No created resource id found in last response")
    assertEquals(createdResourceId, event.path("entityId").asText())
  }

  @Then("the created product should be synced to location catalog")
  fun verifyCreatedProductSyncedToLocationCatalog() {
    val productName = responseContext.lastResponse?.jsonPath()?.getString("productName")
    assertNotNull(productName, "No created product name found in last response")
    val locationId = catalogSyncLocationId
    assertNotNull(locationId, "Catalog sync location id missing")
    val location = locationRepository.findById(locationId).orElse(null)
    assertNotNull(location, "Catalog sync location was not found in organization schema")
    val locationSchema = location.schemaName
    assertNotNull(locationSchema, "Catalog sync location schema name is null")

    val timeoutAt = System.currentTimeMillis() + TestConstants.Timeouts.KAFKA_SYNC_MS
    while (System.currentTimeMillis() < timeoutAt) {
      val session = SessionContext().apply {
        systemUserId = DEFAULT_ID
        oktaId = "test-kafka-cucumber-user"
        organizationId = DEFAULT_ID
        organizationSchemaName = TestConstants.DEFAULT_ORG_SCHEMA
        this.locationId = locationId
        locationSchemaName = locationSchema
        tenantFilterIsComplete = true
      }
      val found = withSession(session) {
        locationProductRepository.findAllLocationProducts().any { it.productName == productName }
      }
      if (found) return
      Thread.sleep(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS)
    }

    fail("Created product '$productName' was not found in location catalog within timeout")
  }
}
