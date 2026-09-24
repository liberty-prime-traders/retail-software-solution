package me.ezra_home.retail_software_solution.configuration.messaging.kafka

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.Metric
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.Uuid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

/**
 * Backs the KafkaTemplate with a producer that never opens a network connection, so publishing
 * is a true no-op instead of retrying against an unreachable broker. Paired with
 * spring.kafka.listener.auto-startup=false, which stops @KafkaListener containers the same way.
 */
@Configuration
@ConditionalOnProperty(name = ["rts.kafka.enabled"], havingValue = "false")
class NoOpKafkaConfig {

    @Bean
    fun kafkaTemplate(): KafkaTemplate<*, *> = KafkaTemplate(NoOpProducerFactory())
}

private class NoOpProducerFactory : ProducerFactory<Any, Any> {
    override fun createProducer(): Producer<Any, Any> = NoOpProducer()
}

private class NoOpProducer : Producer<Any, Any> {

    override fun send(record: ProducerRecord<Any, Any>): Future<RecordMetadata> = send(record, null)

    override fun send(record: ProducerRecord<Any, Any>, callback: Callback?): Future<RecordMetadata> {
        val metadata = RecordMetadata(
            TopicPartition(record.topic(), 0),
            0L,
            0,
            System.currentTimeMillis(),
            0,
            0
        )
        callback?.onCompletion(metadata, null)
        return CompletableFuture.completedFuture(metadata)
    }

    override fun initTransactions() = Unit
    override fun beginTransaction() = Unit

    @Deprecated("Use sendOffsetsToTransaction with ConsumerGroupMetadata instead", ReplaceWith("sendOffsetsToTransaction(offsets, groupMetadata)"))
    override fun sendOffsetsToTransaction(offsets: Map<TopicPartition, OffsetAndMetadata>, consumerGroupId: String) = Unit
    override fun sendOffsetsToTransaction(offsets: Map<TopicPartition, OffsetAndMetadata>, groupMetadata: ConsumerGroupMetadata) = Unit
    override fun commitTransaction() = Unit
    override fun abortTransaction() = Unit
    override fun flush() = Unit
    override fun partitionsFor(topic: String): List<PartitionInfo> = emptyList()
    override fun metrics(): Map<MetricName, Metric> = emptyMap()
    override fun clientInstanceId(timeout: Duration): Uuid = Uuid.ZERO_UUID
    override fun close() = Unit
    override fun close(timeout: Duration) = Unit
}
