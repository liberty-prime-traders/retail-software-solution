package me.ezra_home.retail_software_solution.cross_tier.expense.store

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBatchBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseBatchRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentStateRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
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

    protected abstract fun saveNewBatch(description: String, sourceType: ExpenseSourceType, sourceReference: String?): ExpenseBatchBase

    protected abstract fun saveNewExpense(
        batchId: UUID,
        sourceType: ExpenseSourceType,
        sourceReference: String?,
        resolvedExpenseRow: ResolvedExpenseRow
    ): ExpenseBase

    protected abstract fun saveNewPayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentBase>

    protected abstract fun saveNewPaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidBase

    protected abstract fun saveNewExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidBase

    override fun createBatch(description: String, sourceType: ExpenseSourceType, sourceReference: String?): ExpenseBatchRecord =
        toRecord(saveNewBatch(description, sourceType, sourceReference))

    override fun findBatchBySource(sourceType: ExpenseSourceType, sourceReference: String): ExpenseBatchRecord? =
        expenseBatchRepository.findBySourceTypeAndSourceReference(sourceType, sourceReference)?.let { toRecord(it) }

    override fun saveExpense(
        batchId: UUID,
        sourceType: ExpenseSourceType,
        sourceReference: String?,
        resolvedExpenseRow: ResolvedExpenseRow
    ): ExpenseRecord = toRecord(saveNewExpense(batchId, sourceType, sourceReference, resolvedExpenseRow))

    override fun savePayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentRecord> =
        saveNewPayments(expensePaymentDrafts).map { toRecord(it) }

    override fun saveExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidRecord =
        toRecord(saveNewExpenseVoid(expenseId, reason))

    override fun savePaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidRecord =
        toRecord(saveNewPaymentVoid(paymentId, reason))

    override fun findExpenseById(expenseId: UUID): ExpenseRecord? =
        expenseRepository.findById(expenseId).map { toRecord(it) }.orElse(null)

    override fun findExpenseByReference(referenceNumber: String): ExpenseRecord? =
        expenseRepository.findByReferenceNumber(referenceNumber)?.let { toRecord(it) }

    override fun findExpensesByReferences(referenceNumbers: Collection<String>): List<ExpenseRecord> =
        expenseRepository.findByReferenceNumberIn(referenceNumbers).map { toRecord(it) }

    override fun findPaymentById(paymentId: UUID): ExpensePaymentRecord? =
        expensePaymentRepository.findById(paymentId).map { toRecord(it) }.orElse(null)

    override fun findPaymentByReference(referenceNumber: String): ExpensePaymentRecord? =
        expensePaymentRepository.findByReferenceNumber(referenceNumber)?.let { toRecord(it) }

    override fun findExpenseVoidById(expenseVoidId: UUID): ExpenseVoidRecord? =
        expenseVoidRepository.findById(expenseVoidId).map { toRecord(it) }.orElse(null)

    override fun findPaymentVoidById(paymentVoidId: UUID): ExpensePaymentVoidRecord? =
        expensePaymentVoidRepository.findById(paymentVoidId).map { toRecord(it) }.orElse(null)

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
            batches = batchEntities.map { toRecord(it) },
            expenses = expenseEntities.map { toRecord(it) },
            payments = paymentEntities.map { toRecord(it) },
            paymentVoids = paymentVoidEntities.map { toRecord(it) },
            expenseVoids = expenseVoidEntities.map { toRecord(it) },
            paymentStates = paymentStateEntities.map { toRecord(it) }
        )
    }

    private fun toRecord(batch: ExpenseBatchBase) = ExpenseBatchRecord(
        batch.id!!, batch.requiredReference(), batch.description, batch.requiredCreatedOn(), batch.requiredCreatedById()
    )

    private fun toRecord(expense: ExpenseBase) = ExpenseRecord(
        expense.id!!, expense.requiredReference(), expense.expenseTypeId, expense.expenseAccountCode, expense.payeeContactId, expense.amount,
        expense.expenseDate, expense.description, expense.sourceType, expense.sourceReference, expense.batchId,
        expense.requiredCreatedOn(), expense.requiredCreatedById()
    )

    private fun toRecord(payment: ExpensePaymentBase) = ExpensePaymentRecord(
        payment.id!!, payment.expenseId,
        payment.requiredReference(),
        payment.paymentMethodId, payment.paymentMethodAccountCode, payment.amount,
        payment.providerReference, payment.paymentDate,
        payment.requiredCreatedOn()
    )

    private fun toRecord(paymentVoid: ExpensePaymentVoidBase) = ExpensePaymentVoidRecord(
        paymentVoid.id!!, paymentVoid.expensePaymentId,
        paymentVoid.reason, paymentVoid.requiredCreatedOn()
    )

    private fun toRecord(paymentState: ExpensePaymentStateBase) = ExpensePaymentStateRecord(
        paymentState.expenseId, paymentState.paymentStatus, paymentState.amountPaid
    )

    private fun toRecord(expenseVoid: ExpenseVoidBase) = ExpenseVoidRecord(
        expenseVoid.id!!, expenseVoid.expenseId,
        expenseVoid.reason, expenseVoid.requiredCreatedOn()
    )
}
