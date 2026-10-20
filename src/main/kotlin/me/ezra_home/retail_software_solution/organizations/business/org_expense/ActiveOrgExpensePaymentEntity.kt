package me.ezra_home.retail_software_solution.organizations.business.org_expense

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID
import me.ezra_home.retail_software_solution.util.model.ViewNames
import org.hibernate.annotations.Immutable

@Entity
@Immutable
@Table(name = ViewNames.ACTIVE_ORG_EXPENSE_PAYMENT)
class ActiveOrgExpensePaymentEntity(

    @Id
    @Column(name = "id")
    var id: UUID,

    @Column(name = "expense_id")
    var expenseId: UUID,

    @Column(name = "amount")
    var amount: BigDecimal
)
