package me.ezra_home.retail_software_solution.cross_tier.expense.store

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBatchBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseBatchDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpenseBatch
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpenseBatchRepositoryBase
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentRepositoryBase
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentStateRepositoryBase
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentVoidRepositoryBase
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpenseRepositoryBase
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpenseVoidRepositoryBase
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

abstract class JpaExpenseStore(
    private val expenseBatchRepository: ExpenseBatchRepositoryBase<out ExpenseBatchBase>,
    private val expenseRepository: ExpenseRepositoryBase<out ExpenseBase>,
    private val expensePaymentRepository: ExpensePaymentRepositoryBase<out ExpensePaymentBase>,
    private val expensePaymentVoidRepository: ExpensePaymentVoidRepositoryBase<out ExpensePaymentVoidBase>,
    private val expenseVoidRepository: ExpenseVoidRepositoryBase<out ExpenseVoidBase>,
    private val expensePaymentStateRepository: ExpensePaymentStateRepositoryBase<out ExpensePaymentStateBase>
) : ExpenseStore {

    protected abstract fun saveNewBatch(newExpenseBatch: NewExpenseBatch): ExpenseBatchBase

    protected abstract fun saveNewExpense(newExpense: NewExpense): ExpenseBase

    protected abstract fun saveNewPayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentBase>

    protected abstract fun saveNewPaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidBase

    protected abstract fun saveNewExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidBase

    override fun createBatch(newExpenseBatch: NewExpenseBatch): ExpenseBatchDto =
        ExpenseDtoMapper.toDto(saveNewBatch(newExpenseBatch))

    override fun findBatchBySource(sourceType: ExpenseSourceType, sourceReference: String): ExpenseBatchDto? =
        expenseBatchRepository.findBySourceTypeAndSourceReference(sourceType, sourceReference)?.let { ExpenseDtoMapper.toDto(it) }

    override fun saveExpense(newExpense: NewExpense): ExpenseDto = ExpenseDtoMapper.toDto(saveNewExpense(newExpense))

    override fun savePayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentDto> =
        saveNewPayments(expensePaymentDrafts).map { ExpenseDtoMapper.toDto(it) }

    override fun saveExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidDto =
        ExpenseDtoMapper.toDto(saveNewExpenseVoid(expenseId, reason))

    override fun savePaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidDto =
        ExpenseDtoMapper.toDto(saveNewPaymentVoid(paymentId, reason))

    override fun findExpenseById(expenseId: UUID): ExpenseDto? =
        expenseRepository.findById(expenseId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun findExpenseByReference(referenceNumber: String): ExpenseDto? =
        expenseRepository.findByReferenceNumber(referenceNumber)?.let { ExpenseDtoMapper.toDto(it) }

    override fun findExpensesByReferences(referenceNumbers: Collection<String>): List<ExpenseDto> =
        expenseRepository.findByReferenceNumberIn(referenceNumbers).map { ExpenseDtoMapper.toDto(it) }

    override fun findPaymentById(paymentId: UUID): ExpensePaymentDto? =
        expensePaymentRepository.findById(paymentId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun findPaymentByReference(referenceNumber: String): ExpensePaymentDto? =
        expensePaymentRepository.findByReferenceNumber(referenceNumber)?.let { ExpenseDtoMapper.toDto(it) }

    override fun findExpenseVoidById(expenseVoidId: UUID): ExpenseVoidDto? =
        expenseVoidRepository.findById(expenseVoidId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun findPaymentVoidById(paymentVoidId: UUID): ExpensePaymentVoidDto? =
        expensePaymentVoidRepository.findById(paymentVoidId).map { ExpenseDtoMapper.toDto(it) }.orElse(null)

    override fun loadForBatch(batchId: UUID): ExpenseAggregate {
        val batchEntity = expenseBatchRepository.findById(batchId)
            .orElseThrow { RtsGenericException("Expense batch $batchId not found") }
        return assemble(listOf(batchEntity), expenseRepository.findByBatchId(batchId))
    }

    override fun loadForExpenses(expenseIds: Collection<UUID>): ExpenseAggregate {
        val expenseEntities = expenseRepository.findAllById(expenseIds)
        val missingExpenseIds = expenseIds.toSet() - expenseEntities.map { it.id!! }.toSet()
        if (missingExpenseIds.isNotEmpty()) throw RtsGenericException("Expense $missingExpenseIds not found")
        return assemble(expenseBatchRepository.findAllById(expenseEntities.map { it.batchId }.toSet()), expenseEntities)
    }

    override fun loadRecent(limit: Int): ExpenseAggregate {
        val expenseEntities = expenseRepository
            .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdOn", "id")))
            .content
        return assemble(expenseBatchRepository.findAllById(expenseEntities.map { it.batchId }.toSet()), expenseEntities)
    }

    private fun assemble(batchEntities: List<ExpenseBatchBase>, expenseEntities: List<ExpenseBase>): ExpenseAggregate {
        val expenseIds = expenseEntities.map { it.id!! }
        val paymentEntities = if (expenseIds.isEmpty()) emptyList() else expensePaymentRepository.findByExpenseIdIn(expenseIds)
        val paymentVoidEntities = if (paymentEntities.isEmpty()) emptyList() else
            expensePaymentVoidRepository.findByExpensePaymentIdIn(paymentEntities.map { it.id!! })
        val expenseVoidEntities = if (expenseIds.isEmpty()) emptyList() else expenseVoidRepository.findByExpenseIdIn(expenseIds)
        val paymentStateEntities = if (expenseIds.isEmpty()) emptyList() else expensePaymentStateRepository.findByExpenseIdIn(expenseIds)
        return ExpenseAggregate(
            batches = batchEntities.map { ExpenseDtoMapper.toDto(it) },
            expenses = expenseEntities.map { ExpenseDtoMapper.toDto(it) },
            payments = paymentEntities.map { ExpenseDtoMapper.toDto(it) },
            paymentVoids = paymentVoidEntities.map { ExpenseDtoMapper.toDto(it) },
            expenseVoids = expenseVoidEntities.map { ExpenseDtoMapper.toDto(it) },
            paymentStates = paymentStateEntities.map { ExpenseDtoMapper.toDto(it) }
        )
    }
}
