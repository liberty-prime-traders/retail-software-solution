package me.ezra_home.retail_software_solution.locations.business.sale_adjustment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
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
    val saleId: UUID,

    @Column(name = "sale_line_id", updatable = false)
    val saleLineId: UUID?,

    @Column(name = "direction", nullable = false, length = 5, updatable = false)
    val direction: AdjustmentDirection,

    @Column(name = "calculation_method", nullable = false, length = 5, updatable = false)
    val calculationMethod: CalculationMethod,

    @Column(name = "value", nullable = false, precision = 19, scale = 4, updatable = false)
    val value: BigDecimal,

    @Column(name = "calculated_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    val calculatedAmount: BigDecimal,

    @Column(name = "adjustment_reason_id", nullable = false, updatable = false)
    val adjustmentReasonId: UUID,

    @Column(name = "note", updatable = false)
    val note: String? = null,

    @Column(name = "approved_by_id", updatable = false)
    val approvedById: UUID? = null

) : ImmutableEntity()
