package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class LedgerMovementReader(
    private val ledgerEntryRepository: LedgerEntryRepository
) {

    fun getMovements(
        from: LocalDate,
        before: LocalDate,
        accountCodes: Set<String>,
        sources: Set<LedgerMovementSource>,
        locationId: UUID?
    ): List<LedgerMovement> {
        val dayStarts = dayStartsFor(from, before)
        val sourceTypeCodes = sources.map { source -> LedgerSourceType.valueOf(source.name).code }.toSet()
        val rangeStart = dayStarts.first()
        val rangeEnd = dayStarts.last()
        val dayStartsLiteral = toArrayLiteral(dayStarts)
        val buckets = if (locationId == null) {
            ledgerEntryRepository.sumMovementsInRange(dayStartsLiteral, rangeStart, rangeEnd, accountCodes, sourceTypeCodes)
        } else {
            ledgerEntryRepository.sumMovementsInRangeForLocation(
                dayStartsLiteral, rangeStart, rangeEnd, accountCodes, sourceTypeCodes, locationId
            )
        }
        return buckets.map { bucketMovement ->
            LedgerMovement(
                localDate = from.plusDays(bucketMovement.bucket - 1L),
                accountCode = bucketMovement.accountCode,
                entryType = EntryType.entries.first { entryType -> entryType.code == bucketMovement.entryTypeCode },
                source = LedgerMovementSource.valueOf(
                    LedgerSourceType.entries.first { sourceType -> sourceType.code == bucketMovement.sourceTypeCode }.name
                ),
                amount = bucketMovement.amount
            )
        }
    }

    fun getAccountMovementsFrom(from: LocalDate, before: LocalDate, accountCodes: Set<String>): List<LedgerAccountMovement> {
        val dayStarts = dayStartsFor(from, before)
        return ledgerEntryRepository.sumAccountMovementsFrom(toArrayLiteral(dayStarts), dayStarts.first(), accountCodes)
            .map { bucketMovement ->
                LedgerAccountMovement(
                    localDate = from.plusDays(bucketMovement.bucket - 1L),
                    accountCode = bucketMovement.accountCode,
                    entryType = EntryType.entries.first { entryType -> entryType.code == bucketMovement.entryTypeCode },
                    amount = bucketMovement.amount
                )
            }
    }

    // Use Instant instead of offsetDateTime to avoid timezone issues when converting to Postgres timestamptz[] literal.
    internal fun dayStartsFor(from: LocalDate, before: LocalDate): List<Instant> =
        generateSequence(from) { date -> date.plusDays(1) }
            .takeWhile { date -> !date.isAfter(before) }
            .map { date -> DateTimes.Offset.atStartOfDayInOrganizationZone(date).toInstant() }
            .toList()

    private fun toArrayLiteral(instants: List<Instant>): String = instants.joinToString(",", "{", "}")
}
