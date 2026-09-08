package me.ezra_home.retail_software_solution.locations.business.sale

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatusConverter
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.SALE)
@HasReference(tableName = TableName.SALE)
class SaleEntity(

    @Column(name = "contact_id", nullable = false)
    var contactId: UUID,

    @Column(name = "sold_by_id")
    var soldById: UUID? = null,

    @Column(name = "date_sold")
    var dateSold: OffsetDateTime? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @Convert(converter = SaleStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: SaleStatus,

    @Convert(converter = PaymentStatusConverter::class)
    @Column(name = "payment_status", nullable = false, length = 5)
    var paymentStatus: PaymentStatus = PaymentStatus.UNPAID,

    @Column(name = "subtotal", precision = 19, scale = 4)
    var subtotal: BigDecimal? = null,

    @Column(name = "line_level_discount_total", precision = 19, scale = 4)
    var lineLevelDiscountTotal: BigDecimal? = null,

    @Column(name = "order_level_discount_total", precision = 19, scale = 4)
    var orderLevelDiscountTotal: BigDecimal? = null,

    @Column(name = "line_level_surcharge_total", precision = 19, scale = 4)
    var lineLevelSurchargeTotal: BigDecimal? = null,

    @Column(name = "order_level_surcharge_total", precision = 19, scale = 4)
    var orderLevelSurchargeTotal: BigDecimal? = null,

    @Column(name = "tax_total", precision = 19, scale = 4)
    var taxTotal: BigDecimal? = null,

    @Column(name = "grand_total", precision = 19, scale = 4)
    var grandTotal: BigDecimal? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

) : HasReferenceEntity() {

    fun discountTotal(): BigDecimal =
        (lineLevelDiscountTotal ?: BigDecimal.ZERO) + (orderLevelDiscountTotal ?: BigDecimal.ZERO)

    private fun surchargeTotal(): BigDecimal =
        (lineLevelSurchargeTotal ?: BigDecimal.ZERO) + (orderLevelSurchargeTotal ?: BigDecimal.ZERO)

    fun payableTotal(): BigDecimal =
        grandTotal ?: ((subtotal ?: BigDecimal.ZERO) - discountTotal() + surchargeTotal())
}
