package me.ezra_home.retail_software_solution.organizations.business.expense_type

import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseTypeValidator(
    private val accountService: AccountService,
    private val expenseTypeRepository: ExpenseTypeRepository,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    fun guardInsertable(expenseTypeInsertDto: ExpenseTypeInsertDto) {
        guardNameAvailable(expenseTypeInsertDto.name, null)
        guardEligibilityNotEmpty(expenseTypeInsertDto.eligiblePayeeTypes.size, expenseTypeInsertDto.eligibleSourceTypes.size)
        accountService.requireActiveExpenseLeafAccount(expenseTypeInsertDto.expenseAccountCode)
    }

    fun guardNameAvailable(name: String, expenseTypeId: UUID?) {
        val requiredName = StringUtils.getValueOrException(name, "Expense type name is required")
        // The name check reads every row and no DB constraint can express isEquivalent, so creates and renames serialize here.
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE_TYPE, EXPENSE_TYPE_NAME_LOCK_KEY)
        expenseTypeRepository.findAll()
            .find { StringUtils.isEquivalent(it.name, requiredName) && it.id != expenseTypeId }
            ?.let { throw RtsGenericException("An expense type named '$requiredName' already exists") }
    }

    fun guardEligibilityNotEmpty(eligiblePayeeTypeCount: Int, eligibleSourceTypeCount: Int) {
        if (eligiblePayeeTypeCount == 0) throw RtsGenericException("At least one eligible payee type is required")
        if (eligibleSourceTypeCount == 0) throw RtsGenericException("At least one eligible entry point is required")
    }

    private companion object {
        const val EXPENSE_TYPE_NAME_LOCK_KEY = "name"
    }
}
