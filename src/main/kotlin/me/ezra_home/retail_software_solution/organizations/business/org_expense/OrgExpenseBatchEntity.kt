package me.ezra_home.retail_software_solution.organizations.business.org_expense

import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBatchBase
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.ORG_EXPENSE_BATCH)
@HasReference(tableName = TableName.ORG_EXPENSE_BATCH)
class OrgExpenseBatchEntity(
    description: String,
    sourceType: ExpenseSourceType,
    sourceReference: String?
) : ExpenseBatchBase(description, sourceType, sourceReference)
