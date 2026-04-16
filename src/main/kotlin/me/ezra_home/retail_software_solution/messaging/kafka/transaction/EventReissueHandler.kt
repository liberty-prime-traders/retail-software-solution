package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.messaging.kafka.common.BaseEvent
import java.util.UUID
import kotlin.reflect.KClass

interface EventReissueHandler {
    val eventType: KClass<out BaseEvent>
    fun reissue(sourceDocumentId: UUID)
}
