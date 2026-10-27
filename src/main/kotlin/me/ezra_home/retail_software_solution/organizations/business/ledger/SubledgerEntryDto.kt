package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.util.business.Money
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SubledgerEntryDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val groupReferenceNumber: String,
    val contactReferenceNumber: String,
    val payableAmount: Money,
    val receivableAmount: Money,
    val runningPayable: BigDecimal,
    val runningReceivable: BigDecimal
)
