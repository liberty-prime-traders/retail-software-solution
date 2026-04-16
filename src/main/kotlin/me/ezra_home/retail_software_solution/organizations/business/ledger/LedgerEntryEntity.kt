package me.ezra_home.retail_software_solution.organizations.business.ledger

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryTypeConverter
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal

@Entity
@Table(name = TableNames.LEDGER_ENTRY)
class LedgerEntryEntity(

    @Column(name = "group_reference_number", nullable = false, updatable = false)
    var groupReferenceNumber: String,

    @Column(name = "account_code", nullable = false, updatable = false, length = 50)
    var accountCode: String,

    @Convert(converter = EntryTypeConverter::class)
    @Column(name = "entry_type", nullable = false, updatable = false, length = 5)
    var entryType: EntryType,

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    var amount: BigDecimal

) : HasCreatorEntity()
