package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.SUPPLIER_PAYMENT_VOID)
@HasReference(tableName = TableName.SUPPLIER_PAYMENT_VOID)
class SupplierPaymentVoidEntity(

    @Column(name = "supplier_payment_id", nullable = false, updatable = false, unique = true)
    var supplierPaymentId: UUID,

    @Column(name = "reason", nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
