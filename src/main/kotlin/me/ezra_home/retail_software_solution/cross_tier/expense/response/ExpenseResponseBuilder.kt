package me.ezra_home.retail_software_solution.cross_tier.expense.response

import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentVoidDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseResponseBuilder(
    private val expenseTypeService: ExpenseTypeService,
    private val contactService: ContactService,
    private val paymentMethodService: PaymentMethodService,
    private val userQualifier: UserQualifier
) {

    fun buildSummaries(expenseAggregate: ExpenseAggregate): List<ExpenseSummaryResponse> {
        if (expenseAggregate.expenses.isEmpty()) return emptyList()
        val expenseTypesById = expenseTypeService.getAll(null).associateBy { it.id }
        val contactsById = contactService.getAllContactDtos().associateBy { it.id }
        val paymentMethodNamesById = paymentMethodService.getNamesById()
        val batchesById = expenseAggregate.batches.associateBy { it.id }
        val paymentsByExpenseId = expenseAggregate.payments.groupBy { it.expenseId }
        val paymentVoidsByPaymentId = expenseAggregate.paymentVoids.associateBy { it.paymentId }
        val voidsByExpenseId = expenseAggregate.expenseVoids.associateBy { it.expenseId }
        val paymentStatesByExpenseId = expenseAggregate.paymentStates.associateBy { it.expenseId }
        return expenseAggregate.expenses.map { expense ->
            val expenseType = expenseTypesById[expense.expenseTypeId]
                ?: throw RtsGenericException("Expense type ${expense.expenseTypeId} not found")
            val batch = batchesById.getValue(expense.batchId)
            val payments = paymentsByExpenseId[expense.id].orEmpty().sortedBy { it.createdOn }
            val paymentState = paymentStatesByExpenseId[expense.id]
                ?: throw RtsGenericException("Payment state for expense ${expense.referenceNumber} not found")
            val expenseVoid = voidsByExpenseId[expense.id]
            ExpenseSummaryResponse(
                reference = expense.referenceNumber,
                expenseTypeName = expenseType.name,
                payeeContactId = expense.payeeContactId,
                payeeDisplayName = contactsById[expense.payeeContactId]?.identity?.displayName.orEmpty(),
                amount = expense.amount,
                expenseDate = expense.expenseDate,
                description = expense.description,
                sourceType = expense.sourceType,
                sourceReference = expense.sourceReference,
                batchReference = batch.referenceNumber,
                batchDescription = batch.description,
                status = paymentState.paymentStatus,
                amountPaid = paymentState.amountPaid,
                balanceRemaining = expense.amount - paymentState.amountPaid,
                voided = expenseVoid != null,
                voidReason = expenseVoid?.reason,
                createdOn = expense.createdOn,
                createdBy = userQualifier.getCreatorFullName(expense.createdById),
                payments = payments.map {
                    toPaymentResponse(it, paymentMethodNamesById.getValue(it.paymentMethodId), paymentVoidsByPaymentId[it.id])
                }
            )
        }
    }

    fun buildSummary(expenseAggregate: ExpenseAggregate, expenseId: UUID): ExpenseSummaryResponse {
        val expense = expenseAggregate.expenses.single { it.id == expenseId }
        return buildSummaries(expenseAggregate).single { it.reference == expense.referenceNumber }
    }

    private fun toPaymentResponse(
        payment: ExpensePaymentDto,
        paymentMethodName: String,
        paymentVoid: ExpensePaymentVoidDto?
    ) = ExpensePaymentResponse(
        reference = payment.referenceNumber,
        amount = payment.amount,
        paymentMethodName = paymentMethodName,
        providerReference = payment.providerReference,
        paymentDate = DateTimes.Local.atOrganizationZone(payment.paymentDate),
        voided = paymentVoid != null,
        voidReason = paymentVoid?.reason,
        voidedOn = paymentVoid?.voidedOn,
        createdOn = payment.createdOn
    )
}
