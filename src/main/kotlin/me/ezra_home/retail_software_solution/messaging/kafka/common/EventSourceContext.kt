package me.ezra_home.retail_software_solution.messaging.kafka.common

sealed class EventSourceContext {
    abstract val orgSchema: String

    data class OrgLevel(
        override val orgSchema: String
    ) : EventSourceContext()

    data class LocationLevel(
        override val orgSchema: String,
        val locationSchema: String
    ) : EventSourceContext()
}
