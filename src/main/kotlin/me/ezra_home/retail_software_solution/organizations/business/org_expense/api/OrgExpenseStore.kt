package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpenseBatch
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpensePaymentStateMaintainer
import me.ezra_home.retail_software_solution.cross_tier.expense.store.JpaExpenseStore
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseBatchEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseBatchRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentStateEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentStateRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentVoidEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseVoidEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseVoidRepository
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class OrgExpenseStore(
    private val orgExpenseBatchRepository: OrgExpenseBatchRepository,
    private val orgExpenseRepository: OrgExpenseRepository,
    private val orgExpensePaymentRepository: OrgExpensePaymentRepository,
    private val orgExpensePaymentVoidRepository: OrgExpensePaymentVoidRepository,
    private val orgExpenseVoidRepository: OrgExpenseVoidRepository,
    private val orgExpensePaymentStateRepository: OrgExpensePaymentStateRepository
) : JpaExpenseStore(
    orgExpenseBatchRepository, orgExpenseRepository, orgExpensePaymentRepository, orgExpensePaymentVoidRepository, orgExpenseVoidRepository,
    orgExpensePaymentStateRepository
) {

    override fun saveNewBatch(newExpenseBatch: NewExpenseBatch) = orgExpenseBatchRepository.save(
        OrgExpenseBatchEntity(newExpenseBatch.description, newExpenseBatch.source.type, newExpenseBatch.source.reference)
    )

    override fun saveNewExpense(newExpense: NewExpense): OrgExpenseEntity {
        val resolvedExpenseRow = newExpense.resolvedExpenseRow
        val orgExpenseEntity = orgExpenseRepository.save(
            OrgExpenseEntity(
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
        )
        orgExpensePaymentStateRepository.save(OrgExpensePaymentStateEntity(expenseId = orgExpenseEntity.id!!))
        return orgExpenseEntity
    }

    override fun refreshPaymentStates(expenseDtos: Collection<ExpenseDto>) =
        ExpensePaymentStateMaintainer.refresh(orgExpensePaymentStateRepository, expenseDtos)

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
