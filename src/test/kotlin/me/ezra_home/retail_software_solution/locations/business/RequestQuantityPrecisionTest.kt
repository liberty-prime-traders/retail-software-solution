package me.ezra_home.retail_software_solution.locations.business

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockLineDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCancelLinesDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineAddDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferLineInsertDto
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.api.StockTransferLineUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class RequestQuantityPrecisionTest {

    private val id = UUID.randomUUID()
    private val threeDecimals = BigDecimal("1.255")
    private val twoDecimalsWithTrailingZeros = BigDecimal("1.2500")

    private fun everyRequestBuiltWith(quantity: BigDecimal): List<() -> Any> = listOf(
        { SaleSessionLineAddDto(id, quantity) },
        { SaleSessionLineUpdateDto(SessionIdentity.mintFreshIdentity(), quantity, id, BigDecimal("1.25")) },
        { PurchaseLineCreateDto(id, quantity, id, BigDecimal("1.00")) },
        { PurchaseLineUpdateDto(id, quantity, id, BigDecimal("1.00")) },
        { PurchaseCancelLinesDto(id, quantity) },
        { PurchaseCancelLinesDto(id, quantity, id) },
        { PurchaseDeliveryLineCreateDto(id, quantity, id, BigDecimal("1.00")) },
        { OpeningStockLineDto(id, quantity, id, BigDecimal("1.00")) },
        { StockTransferLineInsertDto(id, quantity) },
        { StockTransferLineUpdateDto("TRL1", quantity, null) },
    )

    @Test
    fun `a typed quantity with three decimals is a business error in every request that carries one`() {
        everyRequestBuiltWith(threeDecimals).forEach { build ->
            assertThrows(RtsGenericException::class.java) { build() }
        }
    }

    @Test
    fun `a quantity written with trailing zeros beyond two decimals is accepted everywhere`() {
        everyRequestBuiltWith(twoDecimalsWithTrailingZeros).forEach { build -> build() }
    }
}
