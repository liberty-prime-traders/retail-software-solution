package me.ezra_home.retail_software_solution.locations.business.sale

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
@Table(name = TableNames.SALE_LINE)
@HasReference(tableName = TableName.SALE_LINE)
class SaleLineEntity(

    @Column(name = "sale_id", nullable = false)
    var saleId: UUID,

    @Column(name = "location_product_id", nullable = false)
    var locationProductId: UUID,

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    var quantity: BigDecimal,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal,

    @Column(name = "conversion_factor", nullable = false, precision = 19, scale = 10)
    var conversionFactor: BigDecimal

) : HasReferenceEntity()
