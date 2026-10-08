package me.ezra_home.retail_software_solution.organizations.business.org_expense

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentBase
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE_PAYMENT)
@HasReference(tableName = TableName.ORG_EXPENSE_PAYMENT)
class OrgExpensePaymentEntity(
    expenseId: UUID,
    paymentMethodId: UUID,
    paymentMethodAccountCode: String,
    amount: BigDecimal,
    providerReference: String?,
    paymentDate: OffsetDateTime
) : ExpensePaymentBase(expenseId, paymentMethodId, paymentMethodAccountCode, amount, providerReference, paymentDate)
