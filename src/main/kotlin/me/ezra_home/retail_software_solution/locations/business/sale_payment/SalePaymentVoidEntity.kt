package me.ezra_home.retail_software_solution.locations.business.sale_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.SALE_PAYMENT_VOID)
@HasReference(tableName = TableName.SALE_PAYMENT_VOID)
class SalePaymentVoidEntity(

    @Column(name = "sale_payment_id", nullable = false, updatable = false, unique = true)
    var salePaymentId: UUID,

    @Column(name = "reason", nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
