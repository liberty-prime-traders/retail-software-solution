package me.ezra_home.retail_software_solution.messaging.kafka.notifications

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class NotificationEventConsumer {

    @KafkaListener(
        topics = [KafkaConstants.Topics.NOTIFICATIONS],
        groupId = KafkaConstants.ConsumerGroups.Notification.ALERTS
    )
    fun onNotification(event: ConsumerFailureEvent) {
        // TODO: handle consumer failure notifications (e.g. alert ops, persist to failure log, trigger retry)
    }
}
