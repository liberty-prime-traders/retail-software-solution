package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.SUPPLIER_PAYMENT)
@HasReference(tableName = TableName.SUPPLIER_PAYMENT)
class SupplierPaymentEntity(

    @Column(name = "purchase_id", nullable = false, updatable = false)
    var purchaseId: UUID,

    @Column(name = "delivery_id", updatable = false)
    var deliveryId: UUID? = null,

    @Column(name = "payment_method_id", nullable = false, updatable = false)
    var paymentMethodId: UUID,

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    var amount: BigDecimal,

    @Column(name = "payment_date", nullable = false, updatable = false)
    var paymentDate: LocalDate,

    @Column(name = "notes")
    var notes: String? = null

) : ImmutableEntity()
