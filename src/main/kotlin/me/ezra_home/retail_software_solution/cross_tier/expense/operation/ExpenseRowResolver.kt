package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSubmission
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.model.PaymentInstruction
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class ExpenseRowResolver(
    private val expenseTypeService: ExpenseTypeService,
    private val contactService: ContactService,
    private val paymentMethodService: PaymentMethodService,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun guardRowCount(rowCount: Int) {
        if (rowCount == 0) throw RtsGenericException("At least one expense row is required")
        if (rowCount > MAXIMUM_ROWS_PER_REQUEST) {
            throw RtsGenericException("A request may contain at most $MAXIMUM_ROWS_PER_REQUEST expense rows")
        }
    }

    fun resolve(sourceType: ExpenseSourceType, expenseSubmission: ExpenseSubmission): List<ResolvedExpenseRow> {
        guardRowCount(expenseSubmission.rowCommands.size)
        val expenseTypesById = HashMap<UUID, ExpenseTypeDto>()
        val payeesById = HashMap<UUID, ContactDto>()
        val openDates = HashSet<LocalDate>()
        return expenseSubmission.rowCommands.map { rowCommand ->
            val roundedAmount = Decimals.roundToScale4(rowCommand.amount)
            if (roundedAmount <= BigDecimal.ZERO) {
                throw RtsGenericException("Expense amount must be greater than zero")
            }
            val expenseType = expenseTypesById.getOrPut(rowCommand.expenseTypeId) {
                expenseTypeService.getById(rowCommand.expenseTypeId)
            }
            if (sourceType !in expenseType.eligibleSourceTypes) {
                throw RtsGenericException("Expense type ${expenseType.name} cannot be used from this screen")
            }
            val payee = payeesById.getOrPut(rowCommand.payeeContactId) {
                contactService.getContactById(rowCommand.payeeContactId)
            }
            if (payee.contactTypes.intersect(expenseType.eligiblePayeeTypes).isEmpty()) {
                throw RtsGenericException(
                    "The selected payee (${payee.identity.displayName}) is not eligible for expense type: ${expenseType.name}"
                )
            }
            val expenseDate = rowCommand.expenseDateOverride ?: expenseSubmission.expenseDate
            requireOpenPeriod(openDates, expenseDate)
            ResolvedExpenseRow(
                expenseType = expenseType,
                payee = payee,
                amount = roundedAmount,
                description = StringUtils.getValueOrNull(rowCommand.description),
                expenseDate = expenseDate,
                settlement = rowCommand.settlement?.let { resolveSettlement(it, expenseDate, openDates) }
            )
        }
    }

    fun resolveSettlement(
        paymentInstruction: PaymentInstruction,
        defaultPaymentDate: LocalDate,
        openDates: MutableSet<LocalDate> = HashSet()
    ): ResolvedSettlement {
        val paymentMethodAccountCode = paymentMethodService.findAccountCode(paymentInstruction.paymentMethodId)
        val paymentDate = paymentInstruction.paymentDate ?: defaultPaymentDate
        requireOpenPeriod(openDates, paymentDate)
        return ResolvedSettlement(
            paymentMethodId = paymentInstruction.paymentMethodId,
            paymentMethodAccountCode = paymentMethodAccountCode,
            providerReference = StringUtils.getValueOrNull(paymentInstruction.paymentReference),
            paymentDate = paymentDate
        )
    }

    fun requireOpenPeriodToday() = fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())

    private fun requireOpenPeriod(alreadyChecked: MutableSet<LocalDate>, date: LocalDate) {
        if (alreadyChecked.add(date)) fiscalPeriodService.requireOpenForDate(date)
    }

    companion object {
        const val MAXIMUM_ROWS_PER_REQUEST = 300
    }
}
