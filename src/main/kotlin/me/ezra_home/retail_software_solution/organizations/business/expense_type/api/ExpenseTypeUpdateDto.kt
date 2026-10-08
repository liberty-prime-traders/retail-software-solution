package me.ezra_home.retail_software_solution.organizations.business.expense_type.api

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import java.util.UUID

data class ExpenseTypeUpdateDto(
    val id: UUID,
    val name: String? = null,
    val expenseAccountCode: String? = null,
    val eligiblePayeeTypes: Set<ContactType>? = null,
    val eligibleSourceTypes: Set<ExpenseSourceType>? = null
) {
    fun changesAnythingButName(): Boolean =
        expenseAccountCode != null || eligiblePayeeTypes != null || eligibleSourceTypes != null

    fun applyTo(existingExpenseTypeDto: ExpenseTypeDto): ExpenseTypeDto = existingExpenseTypeDto.copy(
        name = name ?: existingExpenseTypeDto.name,
        expenseAccountCode = expenseAccountCode ?: existingExpenseTypeDto.expenseAccountCode,
        eligiblePayeeTypes = eligiblePayeeTypes ?: existingExpenseTypeDto.eligiblePayeeTypes,
        eligibleSourceTypes = eligibleSourceTypes ?: existingExpenseTypeDto.eligibleSourceTypes
    )
}
