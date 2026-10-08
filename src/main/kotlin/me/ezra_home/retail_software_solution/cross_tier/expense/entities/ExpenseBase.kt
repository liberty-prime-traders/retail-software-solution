package me.ezra_home.retail_software_solution.cross_tier.expense.entities

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@MappedSuperclass
abstract class ExpenseBase(

    @Column(name = "expense_type_id", nullable = false, updatable = false)
    var expenseTypeId: UUID,

    @Column(name = "expense_account_code", nullable = false, updatable = false, length = 30)
    var expenseAccountCode: String,

    @Column(name = "payee_contact_id", nullable = false, updatable = false)
    var payeeContactId: UUID,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(name = "expense_date", nullable = false, updatable = false)
    var expenseDate: LocalDate,

    @Column(name = "description", updatable = false)
    var description: String?,

    @Convert(converter = ExpenseSourceTypeConverter::class)
    @Column(name = "source_type", nullable = false, updatable = false, length = 5)
    var sourceType: ExpenseSourceType,

    @Column(name = "source_reference", updatable = false)
    var sourceReference: String?,

    @Column(name = "batch_id", nullable = false, updatable = false)
    var batchId: UUID

) : ImmutableEntity()
