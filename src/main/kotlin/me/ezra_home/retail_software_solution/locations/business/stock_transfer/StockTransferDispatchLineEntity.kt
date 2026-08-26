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

    @Column(name = "quantity_dispatched", nullable = false, precision = 15, scale = 4)
    var quantityDispatched: BigDecimal,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    var unitCost: BigDecimal,

    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 10)
    var conversionFactor: BigDecimal,

    @Column(name = "base_unit_id", nullable = false)
    var baseUnitId: UUID

) : HasReferenceEntity()
