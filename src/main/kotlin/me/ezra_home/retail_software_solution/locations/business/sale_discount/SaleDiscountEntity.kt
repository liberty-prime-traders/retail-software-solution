package me.ezra_home.retail_software_solution.locations.business.sale_discount

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
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
@Table(name = TableNames.SALE_DISCOUNT)
@HasReference(tableName = TableName.SALE_DISCOUNT)
class SaleDiscountEntity(

    @Column(name = "sale_id", nullable = false, updatable = false)
    val saleId: UUID,

    @Column(name = "sale_line_id", updatable = false)
    val saleLineId: UUID?,

    @Column(name = "discount_type", nullable = false, length = 5, updatable = false)
    val discountType: CalculationMethod,

    @Column(name = "value", nullable = false, precision = 19, scale = 4, updatable = false)
    val value: BigDecimal,

    @Column(name = "calculated_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    val calculatedAmount: BigDecimal,

    @Column(name = "description", nullable = false, updatable = false)
    val description: String,

    @Column(name = "approved_by_id", updatable = false)
    val approvedById: UUID?

) : ImmutableEntity()
