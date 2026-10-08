package me.ezra_home.retail_software_solution.locations.business.expense

import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentStateBase
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.EXPENSE_PAYMENT_STATE)
class ExpensePaymentStateEntity(expenseId: UUID) : ExpensePaymentStateBase(expenseId)
