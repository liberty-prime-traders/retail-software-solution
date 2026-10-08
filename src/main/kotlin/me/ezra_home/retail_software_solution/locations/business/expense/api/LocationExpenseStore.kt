package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.store.JpaExpenseStore
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseBatchEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseBatchRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentVoidEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentVoidRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseVoidEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseVoidRepository
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
@TransactionalOnLocationSchema
class LocationExpenseStore(
    private val expenseBatchRepository: ExpenseBatchRepository,
    private val expenseRepository: ExpenseRepository,
    private val expensePaymentRepository: ExpensePaymentRepository,
    private val expensePaymentVoidRepository: ExpensePaymentVoidRepository,
    private val expenseVoidRepository: ExpenseVoidRepository,
    private val entityAdvisoryLock: EntityAdvisoryLock
) : JpaExpenseStore(
    expenseBatchRepository, expenseRepository, expensePaymentRepository, expensePaymentVoidRepository, expenseVoidRepository
) {

    override fun sourceContext(): EventSourceContext = EventSourceContext.LocationLevel(
        orgSchema = SessionContextProvider.getOrganizationSchema(),
        locationSchema = SessionContextProvider.getLocationSchema()
    )

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpense(expenseId: UUID) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseId)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockSourceDocument(sourceDocumentId: UUID) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE_SOURCE, sourceDocumentId)
    }

    override fun saveNewBatch(description: String, sourceType: ExpenseSourceType, sourceReference: String?) =
        expenseBatchRepository.save(ExpenseBatchEntity(description, sourceType, sourceReference))

    override fun saveNewExpense(
        batchId: UUID,
        sourceType: ExpenseSourceType,
        sourceReference: String?,
        resolvedExpenseRow: ResolvedExpenseRow
    ) = expenseRepository.save(
        ExpenseEntity(
            expenseTypeId = resolvedExpenseRow.expenseType.id,
            expenseAccountCode = resolvedExpenseRow.expenseType.expenseAccountCode,
            payeeContactId = resolvedExpenseRow.payee.id,
            amount = resolvedExpenseRow.amount,
            expenseDate = resolvedExpenseRow.expenseDate,
            description = resolvedExpenseRow.description,
            sourceType = sourceType,
            sourceReference = sourceReference,
            batchId = batchId
        )
    )

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
