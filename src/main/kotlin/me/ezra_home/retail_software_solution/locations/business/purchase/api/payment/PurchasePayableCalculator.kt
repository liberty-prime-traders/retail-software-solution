package me.ezra_home.retail_software_solution.locations.business.purchase.api.payment

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

object PurchasePayableCalculator {

  fun calculatePayable(paymentCeiling: BigDecimal, paidTotal: BigDecimal): BigDecimal =
    Decimals.MoneyScale.round(paymentCeiling - paidTotal).max(BigDecimal.ZERO)
}
