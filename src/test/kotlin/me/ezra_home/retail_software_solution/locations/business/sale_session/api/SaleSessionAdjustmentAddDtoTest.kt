package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class SaleSessionAdjustmentAddDtoTest {

    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val reasonId = UUID.randomUUID()

    @Test
    fun `a percentage is read at two decimals`() {
        assertEquals(BigDecimal("7.50"), read("""{"percentage":7.5}""").percentage.toBigDecimal())
    }

    @Test
    fun `a missing percentage is rejected`() {
        assertThrows(Exception::class.java) { read("""{}""") }
    }

    @Test
    fun `a negative, oversized or three-decimal percentage is rejected on the wire`() {
        listOf("""{"percentage":-1}""", """{"percentage":100.01}""", """{"percentage":7.125}""").forEach { json ->
            assertRejected { read(json) }
        }
    }

    @Test
    fun `an adjustment is applied as a discount or a surcharge, never both`() {
        assertRejected { read("""{"percentage":5}""", direction = "BOTH") }
        assertEquals(AdjustmentDirection.SURCHARGE, read("""{"percentage":5}""", direction = "SURCHARGE").direction)
    }

    private fun read(fields: String, direction: String = "DISCOUNT"): SaleSessionAdjustmentAddDto {
        val adjustmentJson = objectMapper.readTree(fields) as ObjectNode
        adjustmentJson.put("adjustmentReasonId", reasonId.toString()).put("direction", direction)
        return objectMapper.treeToValue(adjustmentJson, SaleSessionAdjustmentAddDto::class.java)
    }

    private fun assertRejected(build: () -> Any) {
        val thrown = assertThrows(Exception::class.java) { build() }
        assertTrue(generateSequence<Throwable>(thrown) { it.cause }.any { it is RtsGenericException })
    }
}
