package me.ezra_home.retail_software_solution.messaging.kafka.common

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(using = EventSourceContextDeserializer::class)
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

class EventSourceContextDeserializer : JsonDeserializer<EventSourceContext>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): EventSourceContext {
        val node: JsonNode = p.codec.readTree(p)
        val orgSchema = node.get("orgSchema").asText()
        return if (node.has("locationSchema")) {
            EventSourceContext.LocationLevel(orgSchema, node.get("locationSchema").asText())
        } else {
            EventSourceContext.OrgLevel(orgSchema)
        }
    }
}
