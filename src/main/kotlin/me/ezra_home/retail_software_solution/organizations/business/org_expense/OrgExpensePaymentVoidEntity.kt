package me.ezra_home.retail_software_solution.organizations.business.org_expense

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentVoidBase
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE_PAYMENT_VOID)
@HasReference(tableName = TableName.ORG_EXPENSE_PAYMENT_VOID)
class OrgExpensePaymentVoidEntity(
    expensePaymentId: UUID,
    reason: String
) : ExpensePaymentVoidBase(expensePaymentId, reason)
