package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpenseBatch
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpensePaymentStateMaintainer
import me.ezra_home.retail_software_solution.cross_tier.expense.store.JpaExpenseStore
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseBatchEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseBatchRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentStateEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentStateRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentVoidEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentVoidRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseVoidEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseVoidRepository
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema
class LocationExpenseStore(
    private val expenseBatchRepository: ExpenseBatchRepository,
    private val expenseRepository: ExpenseRepository,
    private val expensePaymentRepository: ExpensePaymentRepository,
    private val expensePaymentVoidRepository: ExpensePaymentVoidRepository,
    private val expenseVoidRepository: ExpenseVoidRepository,
    private val expensePaymentStateRepository: ExpensePaymentStateRepository
) : JpaExpenseStore(
    expenseBatchRepository, expenseRepository, expensePaymentRepository, expensePaymentVoidRepository, expenseVoidRepository,
    expensePaymentStateRepository
) {

    override fun saveNewBatch(newExpenseBatch: NewExpenseBatch) = expenseBatchRepository.save(
        ExpenseBatchEntity(newExpenseBatch.description, newExpenseBatch.source.type, newExpenseBatch.source.reference)
    )

    override fun saveNewExpenses(newExpenses: List<NewExpense>): List<ExpenseEntity> {
        val expenseEntitys = expenseRepository.saveAll(
            newExpenses.map { newExpense ->
                val resolvedExpenseRow = newExpense.resolvedExpenseRow
                ExpenseEntity(
                    expenseTypeId = resolvedExpenseRow.expenseType.id,
                    expenseAccountCode = resolvedExpenseRow.expenseType.expenseAccountCode,
                    payeeContactId = resolvedExpenseRow.payee.id,
                    amount = resolvedExpenseRow.amount,
                    expenseDate = resolvedExpenseRow.expenseDate,
                    description = resolvedExpenseRow.description,
                    sourceType = newExpense.source.type,
                    sourceReference = newExpense.source.reference,
                    batchId = newExpense.batchId
                )
            }
        )
        expensePaymentStateRepository.saveAll(expenseEntitys.map { ExpensePaymentStateEntity(expenseId = it.id!!) })
        return expenseEntitys
    }

    override fun refreshPaymentStates(expenseDtos: Collection<ExpenseDto>) =
        ExpensePaymentStateMaintainer.refresh(expensePaymentStateRepository, expenseDtos)

    override fun saveNewPayments(expensePaymentDrafts: List<ExpensePaymentDraft>) =
        expensePaymentRepository.saveAll(
            expensePaymentDrafts.map { expensePaymentDraft ->
                ExpensePaymentEntity(
                    expenseId = expensePaymentDraft.expenseId,
                    paymentMethodId = expensePaymentDraft.resolvedSettlement.paymentMethodId,
                    paymentMethodAccountCode = expensePaymentDraft.resolvedSettlement.paymentMethodAccountCode,
                    amount = expensePaymentDraft.amount,
                    providerReference = expensePaymentDraft.resolvedSettlement.providerReference,
                    paymentDate = DateTimes.Offset.atStartOfDayInOrganizationZone(expensePaymentDraft.resolvedSettlement.paymentDate)
                )
            }
        )

    override fun saveNewPaymentVoid(paymentId: UUID, reason: String) =
        expensePaymentVoidRepository.save(ExpensePaymentVoidEntity(paymentId, reason))

    override fun saveNewExpenseVoid(expenseId: UUID, reason: String) =
        expenseVoidRepository.save(ExpenseVoidEntity(expenseId, reason))
}
