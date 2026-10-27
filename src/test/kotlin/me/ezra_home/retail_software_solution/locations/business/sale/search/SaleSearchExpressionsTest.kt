package me.ezra_home.retail_software_solution.locations.business.sale.search

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.sql.DriverManager
import java.util.UUID

// SaleSearchExpressions deliberately duplicates SaleEntity's total formulas in raw SQL (its own
// header comment says so) because the search grid runs native SQL, not JPA entity logic. Nothing
// else enforces the two stay in sync, so this executes the actual SQL constants -- not a
// reimplementation of them -- against a literal one-row table and asserts the result against
// SaleEntity's Kotlin computation for the same inputs. A formula edited in only one place fails
// here instead of silently drifting between the grid and the entity.
class SaleSearchExpressionsTest {

    @Test
    fun `baseline sale with no discount, surcharge, or tax billed`() {
        assertSqlMatchesEntity(
            subtotal = BigDecimal("100.00"),
            lineLevelDiscountTotal = BigDecimal.ZERO,
            orderLevelDiscountTotal = BigDecimal.ZERO,
            lineLevelSurchargeTotal = BigDecimal.ZERO,
            orderLevelSurchargeTotal = BigDecimal.ZERO,
            taxBilled = BigDecimal.ZERO
        )
    }

    @Test
    fun `sale with discounts and surcharges but no tax billed yet`() {
        assertSqlMatchesEntity(
            subtotal = BigDecimal("100.00"),
            lineLevelDiscountTotal = BigDecimal("10.00"),
            orderLevelDiscountTotal = BigDecimal("5.00"),
            lineLevelSurchargeTotal = BigDecimal("3.00"),
            orderLevelSurchargeTotal = BigDecimal("2.00"),
            taxBilled = BigDecimal.ZERO
        )
    }

    @Test
    fun `confirmed sale with tax billed separately`() {
        assertSqlMatchesEntity(
            subtotal = BigDecimal("100.00"),
            lineLevelDiscountTotal = BigDecimal("10.00"),
            orderLevelDiscountTotal = BigDecimal("5.00"),
            lineLevelSurchargeTotal = BigDecimal("3.00"),
            orderLevelSurchargeTotal = BigDecimal("2.00"),
            taxBilled = BigDecimal("7.50")
        )
    }

    @Test
    fun `zero subtotal with explicit zero tax billed`() {
        assertSqlMatchesEntity(
            subtotal = BigDecimal("0.00"),
            lineLevelDiscountTotal = BigDecimal.ZERO,
            orderLevelDiscountTotal = BigDecimal.ZERO,
            lineLevelSurchargeTotal = BigDecimal.ZERO,
            orderLevelSurchargeTotal = BigDecimal.ZERO,
            taxBilled = BigDecimal("0.00")
        )
    }

    private fun assertSqlMatchesEntity(
        subtotal: BigDecimal,
        lineLevelDiscountTotal: BigDecimal,
        orderLevelDiscountTotal: BigDecimal,
        lineLevelSurchargeTotal: BigDecimal,
        orderLevelSurchargeTotal: BigDecimal,
        taxBilled: BigDecimal
    ) {
        val saleEntity = SaleEntity(
            contactId = UUID.randomUUID(),
            status = SaleStatus.CONFIRMED,
            paymentStatus = PaymentStatus.UNPAID,
            subtotal = subtotal,
            lineLevelDiscountTotal = lineLevelDiscountTotal,
            orderLevelDiscountTotal = orderLevelDiscountTotal,
            lineLevelSurchargeTotal = lineLevelSurchargeTotal,
            orderLevelSurchargeTotal = orderLevelSurchargeTotal,
            taxBilled = taxBilled
        )

        val (taxableAmountFromSql, receivableTotalFromSql) = evaluateSql(
            subtotal, lineLevelDiscountTotal, orderLevelDiscountTotal,
            lineLevelSurchargeTotal, orderLevelSurchargeTotal, taxBilled
        )

        assertEquals(0, saleEntity.taxableAmount().compareTo(taxableAmountFromSql),
            "TAXABLE_AMOUNT SQL (${taxableAmountFromSql}) must match SaleEntity.taxableAmount() (${saleEntity.taxableAmount()})")
        assertEquals(0, saleEntity.receivableTotal().compareTo(receivableTotalFromSql),
            "RECEIVABLE_TOTAL SQL (${receivableTotalFromSql}) must match SaleEntity.receivableTotal() (${saleEntity.receivableTotal()})")
    }

    private fun evaluateSql(
        subtotal: BigDecimal,
        lineLevelDiscountTotal: BigDecimal,
        orderLevelDiscountTotal: BigDecimal,
        lineLevelSurchargeTotal: BigDecimal,
        orderLevelSurchargeTotal: BigDecimal,
        taxBilled: BigDecimal
    ): Pair<BigDecimal, BigDecimal> {
        val sql = """
            SELECT
                ${SaleSearchExpressions.TAXABLE_AMOUNT} AS taxable_amount,
                ${SaleSearchExpressions.RECEIVABLE_TOTAL} AS receivable_total
            FROM (
                SELECT
                    CAST(? AS DECIMAL(19,4)) AS subtotal,
                    CAST(? AS DECIMAL(19,4)) AS line_level_discount_total,
                    CAST(? AS DECIMAL(19,4)) AS order_level_discount_total,
                    CAST(? AS DECIMAL(19,4)) AS line_level_surcharge_total,
                    CAST(? AS DECIMAL(19,4)) AS order_level_surcharge_total,
                    CAST(? AS DECIMAL(19,4)) AS tax_billed
            ) AS ${SaleSearchAliases.SALE}
        """.trimIndent()

        DriverManager.getConnection("jdbc:h2:mem:sale_search_expressions_test;DB_CLOSE_DELAY=-1").use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBigDecimal(1, subtotal)
                statement.setBigDecimal(2, lineLevelDiscountTotal)
                statement.setBigDecimal(3, orderLevelDiscountTotal)
                statement.setBigDecimal(4, lineLevelSurchargeTotal)
                statement.setBigDecimal(5, orderLevelSurchargeTotal)
                statement.setBigDecimal(6, taxBilled)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    return resultSet.getBigDecimal("taxable_amount") to resultSet.getBigDecimal("receivable_total")
                }
            }
        }
    }
}
