package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class LedgerEntryDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val groupReferenceNumber: String,
    val accountCode: String,
    val entryType: EntryType,
    val amount: BigDecimal
)
