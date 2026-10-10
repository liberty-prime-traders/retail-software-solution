package me.ezra_home.retail_software_solution.organizations.business.expense_type.api

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import java.util.Optional
import java.util.UUID

data class ExpenseTypeUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val expenseAccountCode: Optional<String>? = null,
    val eligiblePayeeTypes: Optional<Set<ContactType>>? = null,
    val eligibleSourceTypes: Optional<Set<ExpenseSourceType>>? = null
) {
    fun applyTo(existingExpenseTypeDto: ExpenseTypeDto): ExpenseTypeDto = existingExpenseTypeDto.copy(
        name = name?.orElse(existingExpenseTypeDto.name) ?: existingExpenseTypeDto.name,
        expenseAccountCode = expenseAccountCode?.orElse(existingExpenseTypeDto.expenseAccountCode) ?: existingExpenseTypeDto.expenseAccountCode,
        eligiblePayeeTypes = eligiblePayeeTypes?.orElse(existingExpenseTypeDto.eligiblePayeeTypes) ?: existingExpenseTypeDto.eligiblePayeeTypes,
        eligibleSourceTypes = eligibleSourceTypes?.orElse(existingExpenseTypeDto.eligibleSourceTypes) ?: existingExpenseTypeDto.eligibleSourceTypes
    )
}
