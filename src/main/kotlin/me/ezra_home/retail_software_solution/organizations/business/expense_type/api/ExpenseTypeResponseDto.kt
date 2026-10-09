package me.ezra_home.retail_software_solution.organizations.business.expense_type.api

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import java.util.UUID

data class ExpenseTypeResponseDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val expenseAccountCode: String,
    val eligiblePayeeTypes: Set<ContactType>,
    val eligibleSourceTypes: Set<ExpenseSourceType>,
    val systemDefined: Boolean
)
