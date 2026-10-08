package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.store.JpaExpenseStore
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseBatchEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseBatchRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentVoidEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseVoidEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseVoidRepository
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class OrgExpenseStore(
    private val orgExpenseBatchRepository: OrgExpenseBatchRepository,
    private val orgExpenseRepository: OrgExpenseRepository,
    private val orgExpensePaymentRepository: OrgExpensePaymentRepository,
    private val orgExpensePaymentVoidRepository: OrgExpensePaymentVoidRepository,
    private val orgExpenseVoidRepository: OrgExpenseVoidRepository,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) : JpaExpenseStore(
    orgExpenseBatchRepository, orgExpenseRepository, orgExpensePaymentRepository, orgExpensePaymentVoidRepository, orgExpenseVoidRepository
) {

    override fun sourceContext(): EventSourceContext =
        EventSourceContext.OrgLevel(orgSchema = SessionContextProvider.getOrganizationSchema())

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpense(expenseId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseId.toString())
    }

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockSourceDocument(sourceDocumentId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE_SOURCE, sourceDocumentId.toString())
    }

    override fun saveNewBatch(description: String, sourceType: ExpenseSourceType, sourceReference: String?) =
        orgExpenseBatchRepository.save(OrgExpenseBatchEntity(description, sourceType, sourceReference))

    override fun saveNewExpense(
        batchId: UUID,
        sourceType: ExpenseSourceType,
        sourceReference: String?,
        resolvedExpenseRow: ResolvedExpenseRow
    ) = orgExpenseRepository.save(
        OrgExpenseEntity(
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
        orgExpensePaymentRepository.saveAll(
            expensePaymentDrafts.map { expensePaymentDraft ->
                OrgExpensePaymentEntity(
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
        orgExpensePaymentVoidRepository.save(OrgExpensePaymentVoidEntity(paymentId, reason))

    override fun saveNewExpenseVoid(expenseId: UUID, reason: String) =
        orgExpenseVoidRepository.save(OrgExpenseVoidEntity(expenseId, reason))
}
