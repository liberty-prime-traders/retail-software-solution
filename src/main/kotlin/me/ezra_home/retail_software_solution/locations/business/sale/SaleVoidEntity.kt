package me.ezra_home.retail_software_solution.locations.business.sale

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.SALE_VOID)
@HasReference(tableName = TableName.SALE_VOID)
class SaleVoidEntity(

    @Column(name = "sale_id", nullable = false, updatable = false, unique = true)
    var saleId: UUID,

    @Column(name = "reason", nullable = false, updatable = false)
    var reason: String

) : ImmutableEntity()
