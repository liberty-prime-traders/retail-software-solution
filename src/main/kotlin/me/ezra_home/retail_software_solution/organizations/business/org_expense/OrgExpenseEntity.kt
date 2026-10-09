package me.ezra_home.retail_software_solution.organizations.business.org_expense

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBase
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_EXPENSE)
@HasReference(tableName = TableName.ORG_EXPENSE)
class OrgExpenseEntity(
    expenseTypeId: UUID,
    expenseAccountCode: String,
    payeeContactId: UUID,
    amount: BigDecimal,
    expenseDate: LocalDate,
    description: String?,
    sourceType: ExpenseSourceType,
    sourceReference: String?,
    batchId: UUID
) : ExpenseBase(expenseTypeId, expenseAccountCode, payeeContactId, amount, expenseDate, description, sourceType, sourceReference, batchId)
