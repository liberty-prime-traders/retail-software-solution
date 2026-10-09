package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class OrgExpenseTier(
    override val expenseStore: OrgExpenseStore,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) : ExpenseTier {

    override fun sourceContext(): EventSourceContext =
        EventSourceContext.OrgLevel(orgSchema = SessionContextProvider.getOrganizationSchema())

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpense(expenseId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseId.toString())
    }

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpenses(expenseIds: Collection<UUID>) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseIds.map { it.toString() })
    }

    @TransactionalOnOrganizationSchema(propagation = Propagation.MANDATORY)
    override fun lockSourceDocument(sourceDocumentId: UUID) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.EXPENSE_SOURCE, sourceDocumentId.toString())
    }
}
