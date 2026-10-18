package me.ezra_home.retail_software_solution.locations.business.sale

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.SALE_LINE)
@HasReference(tableName = TableName.SALE_LINE)
class SaleLineEntity(

    @Column(name = "sale_id", nullable = false)
    var saleId: UUID,

    @Column(name = "location_product_id", nullable = false)
    override var locationProductId: UUID,

    @Column(name = "user_provided_quantity", nullable = false, precision = 19, scale = 4)
    override var userProvidedQuantity: BigDecimal,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    // Gross, before any price override; the unit price is derived and never stored
    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    override var lineTotal: BigDecimal,

    @Column(name = "default_sale_price", nullable = false, precision = 28, scale = 10)
    var defaultSalePrice: BigDecimal,

    @Column(name = "storage_units_per_unit", nullable = false)
    var storageUnitsPerUnit: Long,

    @Column(name = "cogs_total", precision = 19, scale = 4)
    var cogsTotal: BigDecimal? = null

) : HasReferenceEntity(), ProductLineWithPrice {

    fun storageQuantity(): ExactQuantity = ExactQuantity.ofUserProvided(userProvidedQuantity, storageUnitsPerUnit)
}
