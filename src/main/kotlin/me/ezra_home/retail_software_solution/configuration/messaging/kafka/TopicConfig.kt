package me.ezra_home.retail_software_solution.configuration.messaging.kafka

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
internal class TopicConfig {

    @Bean
    fun catalogEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaConstants.Topics.CATALOG_EVENTS)
            .partitions(6)
            .replicas(1)
            .build()
    }

    @Bean
    fun transactionEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaConstants.Topics.TRANSACTION_EVENTS)
            .partitions(6)
            .replicas(1)
            .build()
    }
}
