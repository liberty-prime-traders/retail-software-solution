package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCostStatus
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCostStatusConverter
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_ADJUSTMENT_COST)
@HasReference(tableName = TableName.STOCK_ADJUSTMENT_COST)
class StockAdjustmentCostEntity(

    @Column(name = "stock_adjustment_id", nullable = false, updatable = false)
    var stockAdjustmentId: UUID,

    @Column(name = "total_cost", nullable = false, precision = 28, scale = 4)
    var totalCost: BigDecimal,

    @Convert(converter = StockAdjustmentCostStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: StockAdjustmentCostStatus = StockAdjustmentCostStatus.ACTIVE

) : HasReferenceEntity()
