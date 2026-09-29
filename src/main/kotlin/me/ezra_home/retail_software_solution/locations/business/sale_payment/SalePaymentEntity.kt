package me.ezra_home.retail_software_solution.locations.business.sale_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.SALE_PAYMENT)
@HasReference(tableName = TableName.SALE_PAYMENT)
class SalePaymentEntity(

    @Column(name = "sale_id", nullable = false, updatable = false)
    var saleId: UUID,

    @Column(name = "payment_method_id", nullable = false, updatable = false)
    var paymentMethodId: UUID,

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    var amount: BigDecimal,

    @Column(name = "reference", updatable = false)
    var reference: String? = null,

    @Column(name = "payment_date", updatable = false)
    var paymentDate: OffsetDateTime

) : ImmutableEntity()
