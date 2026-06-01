package me.ezra_home.retail_software_solution.locations.business.sale_adjustment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.SALE_ADJUSTMENT)
@HasReference(tableName = TableName.SALE_ADJUSTMENT)
class SaleAdjustmentEntity(

    @Column(name = "sale_id", nullable = false, updatable = false)
    var saleId: UUID,

    @Column(name = "sale_line_id")
    var saleLineId: UUID?,

    @Column(name = "direction", nullable = false, length = 5, updatable = false)
    var direction: AdjustmentDirection,

    @Column(name = "calculation_method", nullable = false, length = 5, updatable = false)
    var calculationMethod: CalculationMethod,

    @Column(name = "value", nullable = false, precision = 19, scale = 4, updatable = false)
    var value: BigDecimal,

    @Column(name = "calculated_amount", nullable = false, precision = 19, scale = 4)
    var calculatedAmount: BigDecimal,

    @Column(name = "adjustment_reason_id", nullable = false, updatable = false)
    var adjustmentReasonId: UUID,

    @Column(name = "note", updatable = false)
    var note: String? = null,

    @Column(name = "approved_by_id", updatable = false)
    var approvedById: UUID? = null

) : HasReferenceEntity()
