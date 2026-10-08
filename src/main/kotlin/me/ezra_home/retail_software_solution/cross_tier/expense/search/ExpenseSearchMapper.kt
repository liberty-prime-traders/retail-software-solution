package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus

object ExpenseSearchMapper {

    fun toBucket(voided: Boolean, paymentStatus: PaymentStatus): ExpenseSummaryBucket = when {
        voided -> ExpenseSummaryBucket.VOIDED
        paymentStatus == PaymentStatus.UNPAID -> ExpenseSummaryBucket.UNPAID
        paymentStatus == PaymentStatus.PARTIALLY_SETTLED -> ExpenseSummaryBucket.PARTIAL
        else -> ExpenseSummaryBucket.PAID
    }
}
