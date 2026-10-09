package me.ezra_home.retail_software_solution.cross_tier.expense.entities

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import java.util.UUID

@MappedSuperclass
abstract class ExpensePaymentVoidBase(

    @Column(name = "expense_payment_id", nullable = false, updatable = false, unique = true)
    var expensePaymentId: UUID,

    @Column(name = "reason", nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
