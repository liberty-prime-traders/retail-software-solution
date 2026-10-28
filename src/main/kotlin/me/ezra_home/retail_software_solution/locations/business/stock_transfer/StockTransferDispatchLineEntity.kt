package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

// One row per cost group of a draft line
@Audited
@Entity
@Table(name = TableNames.STOCK_TRANSFER_DISPATCH_LINE)
@HasReference(tableName = TableName.STOCK_TRANSFER_DISPATCH_LINE)
class StockTransferDispatchLineEntity(

    @Column(name = "stock_transfer_dispatch_id", nullable = false, updatable = false)
    var stockTransferDispatchId: UUID,

    @Column(name = "product_id", nullable = false, updatable = false)
    var orgProductId: UUID,

    @Column(name = "location_product_id", nullable = false)
    var locationProductId: UUID,

    // Named whole-line because it is copied onto every cost-group row,
    // not split between them: summing it across rows overcounts. The row's own share is storageQuantityDispatched
    @Column(name = "whole_line_user_provided_quantity", nullable = false, precision = 19, scale = 4)
    var wholeLineUserProvidedQuantity: BigDecimal,

    @Column(name = "storage_quantity_dispatched", nullable = false, precision = 28, scale = 4)
    var storageQuantityDispatched: BigDecimal,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "unit_cost", nullable = false, precision = 28, scale = 10)
    var unitCost: BigDecimal,

    @Column(name = "cost_dispatched", nullable = false, precision = 28, scale = 4)
    var costDispatched: BigDecimal,

    @Column(name = "storage_units_per_user_provided_unit", nullable = false)
    var storageUnitsPerUserProvidedUnit: Long

) : HasReferenceEntity()
