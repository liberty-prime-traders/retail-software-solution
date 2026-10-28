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

@Entity
@Table(name = TableNames.CONTACT_OPENING_BALANCE)
@HasReference(tableName = TableName.CONTACT_OPENING_BALANCE)
class ContactOpeningBalanceEntity(

    @Column(name = "contact_reference_number", nullable = false, updatable = false)
    var contactReferenceNumber: String,

    @Column(name = "subledger_direction", nullable = false, updatable = false)
    @Convert(converter = ContactSubledgerDirectionConverter::class)
    var subledgerDirection: ContactSubledgerDirection,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal

) : ImmutableEntity()
