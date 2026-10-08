package me.ezra_home.retail_software_solution.organizations.business.org_expense

import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE_PAYMENT_STATE)
class OrgExpensePaymentStateEntity(expenseId: UUID) : ExpensePaymentStateBase(expenseId)
