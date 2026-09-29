package me.ezra_home.retail_software_solution.locations.business.opening_stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = TableNames.OPENING_STOCK)
@HasReference(tableName = TableName.OPENING_STOCK)
class OpeningStockEntity(

    @Column(name = "location_product_id", nullable = false, updatable = false, unique = true)
    var locationProductId: UUID,

    @Column(name = "quantity", nullable = false, updatable = false, precision = 15, scale = 3)
    var quantity: BigDecimal,

    @Column(name = "unit_cost", nullable = false, updatable = false, precision = 15, scale = 2)
    var unitCost: BigDecimal

) : ImmutableEntity()
