package me.ezra_home.retail_software_solution.messaging.kafka.common

object KafkaConstants {
    object Topics {
        const val CATALOG_EVENTS = "catalog-events"
        const val TRANSACTION_EVENTS = "transaction-events"
        const val NOTIFICATIONS = "notifications"

        fun transactionDlt(consumerGroup: String) = "$TRANSACTION_EVENTS.$consumerGroup.DLT"
    }

    object ConsumerGroups {
        object Catalog {
            const val SYNC = "catalog-sync-group"
        }

        object Transaction {
            const val INVENTORY = "inventory-group"
            const val ACCOUNTING = "accounting-group"

            val all = listOf(INVENTORY, ACCOUNTING)
        }

        object Notification {
            const val ALERTS = "notification-alerts-group"
        }
    }
}
