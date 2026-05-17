package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.math.BigDecimal

data class SaleSessionTotals(
    val subtotal: BigDecimal,
    val lineLevelDiscountTotal: BigDecimal,
    val orderLevelDiscountTotal: BigDecimal,
    val lineLevelSurchargeTotal: BigDecimal,
    val orderLevelSurchargeTotal: BigDecimal,
    val paymentTotal: BigDecimal,
    val payableTotal: BigDecimal,
    val balance: BigDecimal,
) {
    companion object {
        val ZERO = SaleSessionTotals(
            subtotal = BigDecimal.ZERO,
            lineLevelDiscountTotal = BigDecimal.ZERO,
            orderLevelDiscountTotal = BigDecimal.ZERO,
            lineLevelSurchargeTotal = BigDecimal.ZERO,
            orderLevelSurchargeTotal = BigDecimal.ZERO,
            paymentTotal = BigDecimal.ZERO,
            payableTotal = BigDecimal.ZERO,
            balance = BigDecimal.ZERO,
        )
    }
}
