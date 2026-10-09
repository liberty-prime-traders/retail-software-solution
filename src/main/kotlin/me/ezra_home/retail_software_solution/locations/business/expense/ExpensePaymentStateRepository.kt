package me.ezra_home.retail_software_solution.locations.business.expense

import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentStateRepositoryBase
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExpensePaymentStateRepository : ExpensePaymentStateRepositoryBase<ExpensePaymentStateEntity> {

    @Query(
        """
        select p.expenseId as expenseId, sum(p.amount) as amountPaid
        from ExpensePaymentEntity p
        where p.expenseId in :expenseIds
          and not exists (select 1 from ExpensePaymentVoidEntity v where v.expensePaymentId = p.id)
        group by p.expenseId
        """
    )
    override fun sumActivePaidByExpenseId(@Param("expenseIds") expenseIds: Collection<UUID>): List<ExpensePaymentStateRepositoryBase.ActivePaidAmount>
}
