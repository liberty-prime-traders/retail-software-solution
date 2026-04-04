package me.ezra_home.retail_software_solution.cucumber.support

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Properties
import java.util.UUID

@Component
class KafkaConsumerTestSupport(
    private val kafkaListenerRegistry: KafkaListenerEndpointRegistry,

    @param:Value("\${spring.kafka.bootstrap-servers}")
    private val bootstrapServers: String
) {

  var catalogEventsConsumer: KafkaConsumer<String, String>? = null

  private val adminProperties = Properties().apply {
    put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
  }

  fun subscribeCatalogEventsFromLatest() {
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

    catalogEventsConsumer = consumer
  }

  fun closeCatalogEventsConsumer() {
    catalogEventsConsumer?.close()
    catalogEventsConsumer = null
  }

  fun prepareConsumerScenario() {
    resetCatalogSyncConsumerOffsetsToLatest()
    startKafkaListeners()
  }

  fun stopKafkaListeners() {
    kafkaListenerRegistry.listenerContainers.forEach { container ->
      if (container.isRunning) {
        container.stop()
      }
    }
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

    AdminClient.create(adminProperties).use { admin ->
      val topicDescription = admin.describeTopics(listOf(topic)).allTopicNames().get()[topic] ?: return
      val partitions = topicDescription.partitions().map { TopicPartition(topic, it.partition()) }
      val latestOffsetSpecs = partitions.associateWith { OffsetSpec.latest() }
      val latestOffsets = admin.listOffsets(latestOffsetSpecs).all().get()
      val groupOffsets = latestOffsets.mapValues { (_, offsetResult) ->
        OffsetAndMetadata(offsetResult.offset())
      }
      admin.alterConsumerGroupOffsets(KafkaConstants.ConsumerGroups.Catalog.SYNC, groupOffsets).all().get()
    }
  }
}
