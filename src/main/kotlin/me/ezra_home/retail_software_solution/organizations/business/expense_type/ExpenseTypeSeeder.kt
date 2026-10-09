package me.ezra_home.retail_software_solution.organizations.business.expense_type

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.SystemExpenseType
import me.ezra_home.retail_software_solution.organizations.business.org_profile.api.OrgDataSeeder
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
@Order(OrgDataSeeder.DEFAULT)
class ExpenseTypeSeeder(
    private val expenseTypeRepository: ExpenseTypeRepository
) : OrgDataSeeder {

    override fun seed() {
        val existingCodes = expenseTypeRepository.findAll().mapNotNull { it.code }.toSet()
        val expenseTypesToInsert = SystemExpenseType.entries
            .filter { it.code !in existingCodes }
            .map { systemExpenseType ->
                ExpenseTypeEntity(
                    code = systemExpenseType.code,
                    name = systemExpenseType.displayName,
                    expenseAccountCode = systemExpenseType.expenseAccount.code,
                    eligiblePayeeTypes = systemExpenseType.eligiblePayeeTypes,
                    eligibleSourceTypes = systemExpenseType.eligibleSourceTypes,
                    systemDefined = true
                )
            }
        if (expenseTypesToInsert.isNotEmpty()) {
            expenseTypeRepository.saveAll(expenseTypesToInsert)
        }
    }
}
