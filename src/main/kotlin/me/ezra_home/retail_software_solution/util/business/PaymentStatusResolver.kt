package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal

object PaymentStatusResolver {

    // The active payment views do not see the transaction's pending writes: read the paid total once before
    // writing and move it with these, never re-read it afterwards.
    fun paidTotalAfterRecording(paidTotalBeforeWrite: BigDecimal, recordedAmounts: Collection<BigDecimal>): BigDecimal =
        paidTotalBeforeWrite + recordedAmounts.sumOf { recordedAmount -> recordedAmount }

    fun paidTotalAfterVoid(paidTotalBeforeVoid: BigDecimal, voidedAmount: BigDecimal): BigDecimal =
        paidTotalBeforeVoid - voidedAmount

    fun resolve(paid: BigDecimal, totalDue: BigDecimal): PaymentStatus = when {
        paid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
        paid > totalDue -> PaymentStatus.OVERPAID
        paid < totalDue -> PaymentStatus.PARTIALLY_SETTLED
        else -> PaymentStatus.FULLY_SETTLED
    }
}
