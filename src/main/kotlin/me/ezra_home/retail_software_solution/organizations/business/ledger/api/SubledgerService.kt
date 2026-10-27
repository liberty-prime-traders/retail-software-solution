package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerMapper
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.SearchGuards
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class SubledgerService(
    private val subledgerEntryRepository: SubledgerEntryRepository,
    private val subledgerBalanceRepository: SubledgerBalanceRepository,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    private val ledgerMapper: LedgerMapper,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalOnOrganizationSchema
    fun reconcileBalances(contactReferenceNumbers: Set<String>) {
        if (contactReferenceNumbers.isEmpty()) return
        orgEntityAdvisoryLock.acquire(LockNamespaces.SUBLEDGER_CONTACT, contactReferenceNumbers)
        val amountTotalsByContact = subledgerEntryRepository.sumAmountsForContacts(contactReferenceNumbers)
            .associateBy { it.contactReferenceNumber }
        val balancesByContact = subledgerBalanceRepository.findByContactReferenceNumberIn(contactReferenceNumbers)
            .associateBy { it.contactReferenceNumber }

        contactReferenceNumbers.forEach { contactReferenceNumber ->
            val expectedPayable = amountTotalsByContact[contactReferenceNumber]?.payableTotal ?: BigDecimal.ZERO
            val expectedReceivable = amountTotalsByContact[contactReferenceNumber]?.receivableTotal ?: BigDecimal.ZERO
            val subledgerBalanceEntity = balancesByContact[contactReferenceNumber]
            when {
                subledgerBalanceEntity == null -> subledgerBalanceRepository.save(
                    SubledgerBalanceEntity(contactReferenceNumber, expectedPayable, expectedReceivable)
                )
                subledgerBalanceEntity.runningPayable.compareTo(expectedPayable) != 0 ||
                    subledgerBalanceEntity.runningReceivable.compareTo(expectedReceivable) != 0 -> {
                    log.warn(
                        "Corrected subledger balance for contact {}: payable {} -> {}, receivable {} -> {}",
                        contactReferenceNumber,
                        subledgerBalanceEntity.runningPayable, expectedPayable,
                        subledgerBalanceEntity.runningReceivable, expectedReceivable
                    )
                    subledgerBalanceEntity.runningPayable = expectedPayable
                    subledgerBalanceEntity.runningReceivable = expectedReceivable
                }
            }
        }
    }

    @TransactionalOnOrganizationSchema
    fun openBalance(contactReferenceNumber: String) {
        subledgerBalanceRepository.save(
            SubledgerBalanceEntity(
                contactReferenceNumber = contactReferenceNumber,
                runningPayable = SubledgerBalanceDto.ZERO.payable,
                runningReceivable = SubledgerBalanceDto.ZERO.receivable
            )
        )
    }

    fun getCurrentBalances(contactReferenceNumbers: Set<String>): Map<String, SubledgerBalanceDto> {
        if (contactReferenceNumbers.isEmpty()) return emptyMap()
        return subledgerBalanceRepository.findByContactReferenceNumberIn(contactReferenceNumbers)
            .associate { subledgerBalanceEntity ->
                subledgerBalanceEntity.contactReferenceNumber to SubledgerBalanceDto(
                    payable = subledgerBalanceEntity.runningPayable,
                    receivable = subledgerBalanceEntity.runningReceivable
                )
            }
    }

    fun getEntries(
        contactReferenceNumber: String,
        previousCursor: String,
        requestedSize: Int
    ): PageResponse<SubledgerEntryResponse, String> {
        SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val fetchedEntries = fetchPageWithLookahead(contactReferenceNumber, previousCursor, requestedSize)
        val pageEntries = fetchedEntries.take(requestedSize)
        val contents = if (pageEntries.isEmpty()) emptyList() else toResponses(pageEntries)

        val currentCursor = contents.lastOrNull()
            ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
            ?: previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = fetchedEntries.size > requestedSize,
            contents = contents
        )
    }

    private fun fetchPageWithLookahead(
        contactReferenceNumber: String,
        previousCursor: String,
        requestedSize: Int
    ): List<SubledgerEntryEntity> {
        val keysetSearchCursor = KeysetSearchCursor.decode(previousCursor)
            ?: return subledgerEntryRepository.findFirstPage(contactReferenceNumber, requestedSize + 1)
        return subledgerEntryRepository.findPageAfter(
            contactReferenceNumber,
            keysetSearchCursor.createdOn,
            keysetSearchCursor.id,
            requestedSize + 1
        )
    }

    private fun toResponses(entryEntities: List<SubledgerEntryEntity>): List<SubledgerEntryResponse> {
        val groupsByReferenceNumber = ledgerEntryGroupRepository
            .findByReferenceNumberIn(entryEntities.map { it.groupReferenceNumber }.toSet())
            .map { ledgerMapper.toGroupDto(it) }
            .associateBy { it.referenceNumber }

        return entryEntities.map { entryEntity ->
            val subledgerEntryDto = ledgerMapper.toSubledgerDto(entryEntity)
            val groupDto = groupsByReferenceNumber.getValue(subledgerEntryDto.groupReferenceNumber)
            val transactionDelta = SubledgerBalanceDto(
                payable = subledgerEntryDto.payableAmount.toBigDecimal(),
                receivable = subledgerEntryDto.receivableAmount.toBigDecimal()
            )
            val runningBalance = SubledgerBalanceDto(
                payable = subledgerEntryDto.runningPayable,
                receivable = subledgerEntryDto.runningReceivable
            )
            SubledgerEntryResponse(
                id = subledgerEntryDto.id,
                createdOn = subledgerEntryDto.createdOn,
                groupReferenceNumber = subledgerEntryDto.groupReferenceNumber,
                sourceType = groupDto.sourceType,
                sourceReferenceNumber = groupDto.sourceReferenceNumber,
                transactionAmount = transactionDelta.netBalance,
                transactionBalanceEffect = transactionDelta.balanceEffect,
                netBalance = runningBalance.netBalance,
                netBalanceEffect = runningBalance.balanceEffect
            )
        }
    }

    private companion object {
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 200
    }
}
