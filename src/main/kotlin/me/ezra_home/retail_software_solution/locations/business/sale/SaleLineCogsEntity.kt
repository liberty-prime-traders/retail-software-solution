package me.ezra_home.retail_software_solution.locations.business.sale

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsStatusConverter
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.SALE_LINE_COGS)
@HasReference(tableName = TableName.SALE_LINE_COGS)
class SaleLineCogsEntity(

    @Column(name = "sale_line_id", nullable = false, updatable = false)
    var saleLineId: UUID,

    @Column(name = "cogs_total", nullable = false, precision = 19, scale = 4)
    var cogsTotal: BigDecimal,

    @Convert(converter = SaleLineCogsStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: SaleLineCogsStatus = SaleLineCogsStatus.ACTIVE

) : HasReferenceEntity()
