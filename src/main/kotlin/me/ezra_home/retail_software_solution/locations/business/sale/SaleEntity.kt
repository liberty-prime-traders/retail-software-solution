package me.ezra_home.retail_software_solution.locations.business.sale

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import me.ezra_home.retail_software_solution.util.enums.PaymentStatusConverter
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
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

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    var subtotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "line_level_discount_total", nullable = false, precision = 19, scale = 4)
    var lineLevelDiscountTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "order_level_discount_total", nullable = false, precision = 19, scale = 4)
    var orderLevelDiscountTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "line_level_surcharge_total", nullable = false, precision = 19, scale = 4)
    var lineLevelSurchargeTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "order_level_surcharge_total", nullable = false, precision = 19, scale = 4)
    var orderLevelSurchargeTotal: BigDecimal = BigDecimal.ZERO,

    // Null until tax finalization has run; SaleValidator.guardCanVoid relies on that to detect a pending finalization.
    @Column(name = "tax_total", precision = 19, scale = 4)
    var taxTotal: BigDecimal? = null,

    @Column(name = "tax_billed", nullable = false, precision = 19, scale = 4)
    var taxBilled: BigDecimal = BigDecimal.ZERO,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

) : HasReferenceEntity() {

    fun discountTotal(): BigDecimal =
        lineLevelDiscountTotal + orderLevelDiscountTotal

    fun surchargeTotal(): BigDecimal =
        lineLevelSurchargeTotal + orderLevelSurchargeTotal

    fun displaySubtotal(): BigDecimal =
        subtotal + surchargeTotal()

    fun taxableAmount(): BigDecimal =
        displaySubtotal() - discountTotal()

    fun receivableTotal(): BigDecimal =
        taxableAmount() + taxBilled
}
