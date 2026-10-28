package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategoryConverter
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentDirection
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentDirectionConverter
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = TableNames.STOCK_ADJUSTMENT)
@HasReference(tableName = TableName.STOCK_ADJUSTMENT)
class StockAdjustmentEntity(

    @Column(name = "location_product_id", nullable = false, updatable = false)
    var locationProductId: UUID,

    @Column(name = "category", nullable = false, updatable = false, length = 5)
    @Convert(converter = StockAdjustmentCategoryConverter::class)
    var category: StockAdjustmentCategory,

    @Column(name = "direction", nullable = false, updatable = false, length = 5)
    @Convert(converter = StockAdjustmentDirectionConverter::class)
    var direction: StockAdjustmentDirection,

    @Column(name = "user_provided_quantity", nullable = false, updatable = false, precision = 19, scale = 4)
    var userProvidedQuantity: BigDecimal,

    @Column(name = "unit_id", nullable = false, updatable = false)
    var unitId: UUID,

    @Column(name = "storage_units_per_user_provided_unit", nullable = false, updatable = false)
    var storageUnitsPerUserProvidedUnit: Long,

    @Column(name = "storage_quantity", nullable = false, updatable = false, precision = 28, scale = 4)
    var storageQuantity: BigDecimal,

    @Column(name = "total_cost", nullable = false, updatable = false, precision = 28, scale = 4)
    var totalCost: BigDecimal,

    @Column(name = "stock_movement_reason_id", updatable = false)
    var stockMovementReasonId: UUID?,

    @Column(name = "note", updatable = false, length = 500)
    var note: String? = null

) : ImmutableEntity()
