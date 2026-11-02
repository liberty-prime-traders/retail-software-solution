package me.ezra_home.retail_software_solution.locations.business.opening_stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.business.Decimals
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

    @Column(name = "user_provided_quantity", nullable = false, updatable = false, precision = 19, scale = 4)
    var userProvidedQuantity: BigDecimal,

    @Column(name = "unit_id", nullable = false, updatable = false)
    var unitId: UUID,

    @Column(name = "storage_units_per_user_provided_qty_unit", nullable = false, updatable = false)
    var storageUnitsPerUserProvidedQtyUnit: Long,

    @Column(name = "total_cost", nullable = false, updatable = false, precision = 19, scale = 4)
    var totalCost: BigDecimal

) : ImmutableEntity() {

    /** Display figure per [unitId]; [totalCost] is the exact amount and does not round-trip from this. */
    fun unitCost(): BigDecimal = Decimals.Scale4.divide(totalCost, userProvidedQuantity)
}
