package me.ezra_home.retail_software_solution.organizations.business.expense_type.api

import java.util.UUID

data class ExpenseTypeRenameDto(
    val id: UUID,
    val name: String
)
