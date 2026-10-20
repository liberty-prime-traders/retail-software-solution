package me.ezra_home.retail_software_solution.locations.business.sale_payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import me.ezra_home.retail_software_solution.util.model.ViewNames
import org.hibernate.annotations.Immutable

@Entity
@Immutable
@Table(name = ViewNames.ACTIVE_SALE_PAYMENT)
class ActiveSalePaymentEntity(

    @Id
    @Column(name = "id")
    var id: UUID,

    @Column(name = "reference_number")
    var referenceNumber: String,

    @Column(name = "sale_id")
    var saleId: UUID,

    @Column(name = "amount")
    var amount: BigDecimal,

    @Column(name = "created_on")
    var createdOn: OffsetDateTime
)
