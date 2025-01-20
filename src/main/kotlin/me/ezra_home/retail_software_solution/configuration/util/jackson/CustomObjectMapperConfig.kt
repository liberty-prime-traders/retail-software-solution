package me.ezra_home.retail_software_solution.configuration.util.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.OffsetDateTime

@Configuration
class CustomObjectMapperConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        val javaTimeModule = JavaTimeModule()

        javaTimeModule.addSerializer(
            OffsetDateTime::class.java,
            object: JsonSerializer<OffsetDateTime>() {
                override fun serialize(value: OffsetDateTime, gen: JsonGenerator, serializers: SerializerProvider) {
                    val millis = value.toInstant().toEpochMilli()
                    gen.writeNumber(millis)
                }
            }
        )

//        javaTimeModule.addDeserializer(
//            OffsetDateTime::class.java,
//            object : JsonDeserializer<OffsetDateTime>() {
//                @Throws(JsonProcessingException::class)
//                override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OffsetDateTime {
//                    val timestamp = p.longValue
//                    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), java.time.ZoneOffset.UTC)
//                }
//            }
//        )

        objectMapper.registerModule(javaTimeModule)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        return objectMapper
    }
}
