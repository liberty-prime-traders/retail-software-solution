package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class LocationExpenseTier(
    override val expenseStore: LocationExpenseStore,
    private val entityAdvisoryLock: EntityAdvisoryLock
) : ExpenseTier {

    override fun sourceContext(): EventSourceContext = EventSourceContext.LocationLevel(
        orgSchema = SessionContextProvider.getOrganizationSchema(),
        locationSchema = SessionContextProvider.getLocationSchema()
    )

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpense(expenseId: UUID) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseId)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockExpenses(expenseIds: Collection<UUID>) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE, expenseIds)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    override fun lockSourceDocument(sourceDocumentId: UUID) {
        entityAdvisoryLock.acquire(LockNamespaces.EXPENSE_SOURCE, sourceDocumentId)
    }
}
