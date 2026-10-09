package me.ezra_home.retail_software_solution.cross_tier.expense.entities

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.enums.PaymentStatusConverter
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import java.math.BigDecimal
import java.util.UUID

@MappedSuperclass
abstract class ExpensePaymentStateBase(

    @Column(name = "expense_id", nullable = false, updatable = false)
    var expenseId: UUID,

    @Convert(converter = PaymentStatusConverter::class)
    @Column(name = "payment_status", nullable = false, length = 5)
    var paymentStatus: PaymentStatus = PaymentStatus.UNPAID,

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    var amountPaid: BigDecimal = BigDecimal.ZERO,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

) : HasCreatorEntity()
