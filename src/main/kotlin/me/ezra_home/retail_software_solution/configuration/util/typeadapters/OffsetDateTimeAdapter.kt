package me.ezra_home.retail_software_solution.configuration.util.typeadapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal class OffsetDateTimeAdapter : JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {
    override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): OffsetDateTime? {
        if (json == null || json.isJsonNull) {
            return null
        }
        return try {
            OffsetDateTime.parse(json.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            throw JsonParseException("Failed to parse OffsetDateTime: " + json.asString, e)
        }
    }
}
