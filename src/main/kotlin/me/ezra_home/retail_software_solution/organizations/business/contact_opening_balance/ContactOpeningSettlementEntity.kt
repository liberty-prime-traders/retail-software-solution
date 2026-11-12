package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = TableNames.CONTACT_OPENING_SETTLEMENT)
@HasReference(tableName = TableName.CONTACT_OPENING_SETTLEMENT)
class ContactOpeningSettlementEntity(

    @Column(name = "contact_reference_number", nullable = false, updatable = false)
    var contactReferenceNumber: String,

    @Column(name = "subledger_direction", nullable = false, updatable = false)
    @Convert(converter = ContactSubledgerDirectionConverter::class)
    var subledgerDirection: ContactSubledgerDirection,

    @Column(name = "payment_account_code", nullable = false, updatable = false)
    var paymentAccountCode: String,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(name = "settlement_date", nullable = false, updatable = false)
    var settlementDate: LocalDate,

    @Column(name = "note", updatable = false)
    var note: String?

) : ImmutableEntity() {

    fun requiredId(): UUID = requireNotNull(id) { "ContactOpeningSettlementEntity is missing its id" }
}
