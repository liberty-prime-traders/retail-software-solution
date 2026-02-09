package me.ezra_home.retail_software_solution.messaging.kafka.common

object KafkaConstants {
    object Topics {
        const val CATALOG_EVENTS = "catalog-events"
        const val TRANSACTION_EVENTS = "transaction-events"
    }

    object ConsumerGroups {
        const val CATALOG_SYNC = "catalog-sync-group"
    }
}
