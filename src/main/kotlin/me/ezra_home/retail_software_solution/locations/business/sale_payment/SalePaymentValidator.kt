package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.business.Currencies
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal

object SalePaymentValidator {

    fun guardPositiveAmount(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) {
            throw RtsGenericException("Payment amount must be greater than zero")
        }
    }

    fun guardNotExceedingSaleTotal(totalSubmitted: BigDecimal, saleTotal: BigDecimal) {
        if (totalSubmitted > saleTotal)
            throw RtsGenericException(
                "Payments of ${Currencies.format(totalSubmitted)} exceed " +
                        "sale total of ${Currencies.format(saleTotal)}"
            )
    }

    fun guardNotExceedingBalance(amount: BigDecimal, remainingBalance: BigDecimal) {
        if (amount > remainingBalance)
            throw RtsGenericException(
                "Payment of ${Currencies.format(amount)} would exceed remaining" +
                        " balance of ${Currencies.format(remainingBalance)}"
            )
    }

    fun guardNotAlreadyVoided(alreadyVoided: Boolean, referenceNumber: String) {
        if (alreadyVoided) {
            throw RtsGenericException("Payment $referenceNumber has already been voided")
        }
    }

    fun guardSaleNotVoided(saleStatus: SaleStatus) {
        if (saleStatus == SaleStatus.VOIDED)
            throw RtsGenericException("Cannot void a payment on a voided sale")
    }
}
