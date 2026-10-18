package me.ezra_home.retail_software_solution.configuration.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

class StripZeroesAndRound : JsonSerializer<BigDecimal>() {
    override fun serialize(value: BigDecimal, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(Decimals.stripZeroesAndRoundNeverToZero(value))
    }
}
