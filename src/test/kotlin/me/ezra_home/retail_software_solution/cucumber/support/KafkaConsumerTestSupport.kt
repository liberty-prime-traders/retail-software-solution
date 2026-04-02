package me.ezra_home.retail_software_solution.cucumber.support

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component
import java.util.Properties

@Component
class KafkaConsumerTestSupport(
    private val kafkaListenerRegistry: KafkaListenerEndpointRegistry,
    @param:Value("\${spring.kafka.bootstrap-servers}")
  private val bootstrapServers: String
) {

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
    val properties = Properties().apply {
      put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    }

    AdminClient.create(properties).use { admin ->
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
