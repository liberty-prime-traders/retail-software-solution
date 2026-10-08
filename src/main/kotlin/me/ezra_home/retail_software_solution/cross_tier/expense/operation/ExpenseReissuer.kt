package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import java.util.UUID

interface ExpenseReissuer {

    val isLocationLevel: Boolean

    fun reissueRecorded(expenseId: UUID)

    fun reissueVoided(expenseVoidId: UUID)

    fun reissuePaymentRecorded(paymentId: UUID)

    fun reissuePaymentVoided(paymentVoidId: UUID)
}
