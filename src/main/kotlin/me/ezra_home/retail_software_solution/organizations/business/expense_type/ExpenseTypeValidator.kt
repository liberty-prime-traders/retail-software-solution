package me.ezra_home.retail_software_solution.organizations.business.expense_type

import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseTypeValidator(
    private val accountService: AccountService,
    private val expenseTypeRepository: ExpenseTypeRepository
) {

    fun guardInsertable(expenseTypeInsertDto: ExpenseTypeInsertDto) {
        guardNameAvailable(expenseTypeInsertDto.name, null)
        guardEligibilityNotEmpty(expenseTypeInsertDto.eligiblePayeeTypes.size, expenseTypeInsertDto.eligibleSourceTypes.size)
        accountService.requireActiveExpenseLeafAccount(expenseTypeInsertDto.expenseAccountCode)
    }

    fun guardNameAvailable(name: String, expenseTypeId: UUID?) {
        val requiredName = StringUtils.getValueOrException(name, "Expense type name is required")
        expenseTypeRepository.findAll()
            .find { StringUtils.isEquivalent(it.name, requiredName) && it.id != expenseTypeId }
            ?.let { throw RtsGenericException("An expense type named '$requiredName' already exists") }
    }

    fun guardEligibilityNotEmpty(eligiblePayeeTypeCount: Int, eligibleSourceTypeCount: Int) {
        if (eligiblePayeeTypeCount == 0) throw RtsGenericException("At least one eligible payee type is required")
        if (eligibleSourceTypeCount == 0) throw RtsGenericException("At least one eligible entry point is required")
    }
}
