package me.ezra_home.retail_software_solution.cucumber.steps

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.config.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.organizations.business.location.LocationRepository
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationRepository
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import java.time.Duration
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class KafkaSteps {

  companion object {
    private val DEFAULT_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private const val KAFKA_CONSUMER_KEY = "kafka.consumer"
    private const val LAST_CATALOG_EVENT_KEY = "kafka.lastCatalogEvent"
    private const val CATALOG_SYNC_LOCATION_ID_KEY = "kafka.catalogSyncLocationId"
    private val OBJECT_MAPPER = jacksonObjectMapper()
  }

  @Autowired
  private lateinit var context: TestContext

  @Autowired
  private lateinit var requestFactory: AuthenticatedRequestFactory

  @Autowired
  private lateinit var organizationRepository: OrganizationRepository

  @Autowired
  private lateinit var locationRepository: LocationRepository

  @Autowired
  private lateinit var locationProductRepository: LocationProductRepository

  @Autowired
  private lateinit var kafkaListenerRegistry: KafkaListenerEndpointRegistry

  @Value("\${spring.kafka.bootstrap-servers}")
  private lateinit var bootstrapServers: String

  @Given("a location exists for catalog sync")
  fun createLocationForCatalogSync() {
    resetCatalogSyncConsumerOffsetsToLatest()
    startKafkaListeners()
    ensurePublicSchemaOrganizationExists()

    val response = requestFactory.jsonRequest()
      .body(
        mapOf(
          "locationType" to "SHOP",
          "name" to "Catalog Sync Location",
          "description" to "Location used in cucumber kafka consumer flow"
        )
      )
      .post("/secured/locations")

    assertEquals(200, response.statusCode, "Failed to create location for sync. Response: ${response.asString()}")
    val locationId = response.jsonPath().getString("id")
    assertNotNull(locationId, "Location creation response did not contain id")
    context.currentLocationId = UUID.fromString(locationId)
    context.store(CATALOG_SYNC_LOCATION_ID_KEY, UUID.fromString(locationId))
  }

  @Given("I use the catalog sync location context")
  fun useCatalogSyncLocationContext() {
    val locationId = context.get(CATALOG_SYNC_LOCATION_ID_KEY, UUID::class.java)
    assertNotNull(locationId, "Catalog sync location id was not set in test context")
    context.currentLocationId = locationId
  }

  private fun startKafkaListeners() {
    kafkaListenerRegistry.listenerContainers.forEach { container ->
      if (!container.isRunning) {
        container.start()
      }
    }
  }

  private fun resetCatalogSyncConsumerOffsetsToLatest() {
    val topic = KafkaConstants.Topics.CATALOG_EVENTS
    val properties = Properties().apply {
      put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    }

    AdminClient.create(properties).use { admin ->
      val topicDescription = admin.describeTopics(listOf(topic)).allTopicNames().get()[topic]
        ?: return
      val partitions = topicDescription.partitions().map { TopicPartition(topic, it.partition()) }
      val latestOffsetSpecs = partitions.associateWith { OffsetSpec.latest() }
      val latestOffsets = admin.listOffsets(latestOffsetSpecs).all().get()
      val groupOffsets = latestOffsets.mapValues { (_, offsetResult) ->
        OffsetAndMetadata(offsetResult.offset())
      }

      admin.alterConsumerGroupOffsets(
        KafkaConstants.ConsumerGroups.CATALOG_SYNC,
        groupOffsets
      ).all().get()
    }
  }

  private fun ensurePublicSchemaOrganizationExists() {
    val publicSchemaExists = organizationRepository.findAll().any { it.schemaName == "public" }
    if (publicSchemaExists) return

    organizationRepository.save(
      OrganizationEntity(
        name = "Public Schema Test Organization",
        description = "Seeded for kafka consumer cucumber tests",
        subdomain = "public-test",
        schemaName = "public"
      ).apply {
        createdById = DEFAULT_USER_ID
      }
    )
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
    val partitions = consumer.partitionsFor(topic)
      .map { TopicPartition(topic, it.partition()) }
    consumer.assign(partitions)
    consumer.poll(Duration.ofMillis(200))

    val endOffsets = consumer.endOffsets(partitions)
    partitions.forEach { partition ->
      consumer.seek(partition, endOffsets[partition] ?: 0L)
    }

    context.store(KAFKA_CONSUMER_KEY, consumer)
  }

  @Then("a catalog event should be published for table {string}")
  fun verifyCatalogEventPublished(tableName: String) {
    @Suppress("UNCHECKED_CAST")
    val consumer = context.get(KAFKA_CONSUMER_KEY, KafkaConsumer::class.java) as? KafkaConsumer<String, String>
    assertNotNull(consumer, "Kafka consumer was not initialized. Use step: I am subscribed to the catalog events topic")

    val timeoutAt = System.currentTimeMillis() + 15_000
    var matchedEvent: JsonNode? = null

    while (System.currentTimeMillis() < timeoutAt && matchedEvent == null) {
      val records = consumer.poll(Duration.ofMillis(500))
      for (record in records) {
        val payload = OBJECT_MAPPER.readTree(record.value())
        if (payload.path("tableName").asText() == tableName) {
          matchedEvent = payload
          break
        }
      }
    }

    assertNotNull(matchedEvent, "No catalog event received for table $tableName within timeout")
    context.store(LAST_CATALOG_EVENT_KEY, matchedEvent)
  }

  @Then("the catalog event should reference the created resource")
  fun verifyCatalogEventEntityMatchesResponse() {
    val event = context.get(LAST_CATALOG_EVENT_KEY, JsonNode::class.java)
    assertNotNull(event, "No catalog event captured in context")

    val createdResourceId = context.lastResponse?.jsonPath()?.getString("id")
    assertNotNull(createdResourceId, "No created resource id found in last response")
    assertEquals(createdResourceId, event.path("entityId").asText())
  }

  @Then("the created product should be synced to location catalog")
  fun verifyCreatedProductSyncedToLocationCatalog() {
    val productName = context.lastResponse?.jsonPath()?.getString("productName")
    assertNotNull(productName, "No created product name found in last response")
    val locationId = context.get(CATALOG_SYNC_LOCATION_ID_KEY, UUID::class.java)
    assertNotNull(locationId, "Catalog sync location id missing in test context")
    val location = locationRepository.findById(locationId).orElse(null)
    assertNotNull(location, "Catalog sync location was not found in organization schema")
    val locationSchema = location.schemaName
    assertNotNull(locationSchema, "Catalog sync location schema name is null")

    val timeoutAt = System.currentTimeMillis() + 20_000
    while (System.currentTimeMillis() < timeoutAt) {
      val found = withLocationSession(locationId, locationSchema) {
        locationProductRepository.findAllLocationProducts().any { it.productName == productName }
      }
      if (found) {
          return
      }
      Thread.sleep(500)
    }

    fail("Created product '$productName' was not found in location catalog within timeout")
  }

  private fun <T> withLocationSession(locationId: UUID, locationSchema: String, block: () -> T): T {
    val session = SessionContext().apply {
      systemUserId = DEFAULT_USER_ID
      oktaId = "test-kafka-cucumber-user"
      organizationId = DEFAULT_USER_ID
      organizationSchemaName = "public"
      this.locationId = locationId
      locationSchemaName = locationSchema
      tenantFilterIsComplete = true
    }

    SessionContextProvider.setSession(session)
    return try {
      block()
    } finally {
      SessionContextProvider.clear()
    }
  }
}
