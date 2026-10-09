package me.ezra_home.retail_software_solution.locations.business.expense

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentVoidBase
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.EXPENSE_PAYMENT_VOID)
@HasReference(tableName = TableName.EXPENSE_PAYMENT_VOID)
class ExpensePaymentVoidEntity(
    expensePaymentId: UUID,
    reason: String
) : ExpensePaymentVoidBase(expensePaymentId, reason)
