package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.util.model.ViewNames
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.util.UUID

@Entity
@Immutable
@Table(name = ViewNames.ACTIVE_CONTACT_OPENING_SETTLEMENT)
class ActiveContactOpeningSettlementEntity(

    @Id
    @Column(name = "id")
    var id: UUID,

    @Column(name = "contact_reference_number")
    var contactReferenceNumber: String,

    @Column(name = "subledger_direction")
    @Convert(converter = ContactSubledgerDirectionConverter::class)
    var subledgerDirection: ContactSubledgerDirection,

    @Column(name = "amount")
    var amount: BigDecimal
)
