package me.ezra_home.retail_software_solution.messaging.kafka.common

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DltReader<EVENT>(consumerFactory: ConsumerFactory<String, EVENT>) {

    private val consumer by lazy { consumerFactory.createConsumer("dlt-reader", null) }

    @Synchronized
    fun fetchAt(topic: String, partition: Int, offset: Long): ConsumerRecord<String, EVENT>? {
        val topicPartition = TopicPartition(topic, partition)
        consumer.assign(listOf(topicPartition))
        consumer.seek(topicPartition, offset)
        return consumer.poll(Duration.ofSeconds(5)).firstOrNull { it.offset() == offset }
    }
}
