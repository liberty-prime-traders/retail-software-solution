package me.ezra_home.retail_software_solution.locations.business

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockLineDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCreateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

@WithDefaultOrganizationSession
class RequestUnitCostPrecisionTest {

    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val id = UUID.randomUUID()

    private fun everyRequestBuiltWith(unitCost: String): List<() -> Any> = listOf(
        { read<PurchaseLineCreateDto>("""{"locationProductId":"$id","quantityOrdered":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<PurchaseLineUpdateDto>("""{"id":"$id","quantityOrdered":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<PurchaseDeliveryLineCreateDto>("""{"purchaseLineId":"$id","quantityDelivered":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<OpeningStockLineDto>("""{"locationProductId":"$id","quantity":1,"unitId":"$id","unitCost":$unitCost}""") },
        { read<StockAdjustmentCreateDto>("""{"locationProductId":"$id","category":"OTHER","quantity":1,"unitId":"$id","unitCost":$unitCost}""") },
    )

    private inline fun <reified RequestType> read(json: String): RequestType = objectMapper.readValue(json, RequestType::class.java)

    @Test
    fun `a typed unit cost with three decimals is a business error in every request that carries one`() {
        everyRequestBuiltWith("1.255").forEach { build ->
            val thrown = assertThrows(Exception::class.java) { build() }
            assertTrue(generateSequence<Throwable>(thrown) { it.cause }.any { it is RtsGenericException })
        }
    }

    @Test
    fun `a unit cost written with trailing zeros beyond two decimals is accepted everywhere`() {
        everyRequestBuiltWith("1.2500").forEach { build -> build() }
    }
}
