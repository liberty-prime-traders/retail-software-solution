package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
@TransactionalOnOrganizationSchema
class LedgerPostingService(
    private val groupRepository: LedgerEntryGroupRepository,
    private val entryRepository: LedgerEntryRepository,
    private val subledgerRepository: SubledgerEntryRepository,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun post(request: LedgerPostingRequest) {
        val fiscalPeriodId = fiscalPeriodService.findOpenForDate(request.postingDate)
            ?: throw RtsGenericException("No open fiscal period for ${request.postingDate}")

        val group = groupRepository.saveAndFlush(
            LedgerEntryGroupEntity(
                sourceReferenceNumber = request.sourceReferenceNumber,
                sourceType = request.sourceType,
                sourceLocationId = SessionContextProvider.getLocationId(),
                fiscalPeriodId = fiscalPeriodId,
                postedOn = Instant.now()
            )
        )

        entryRepository.saveAll(
            request.entries.map { e ->
                LedgerEntryEntity(
                    groupReferenceNumber = group.referenceNumber!!,
                    accountCode = e.accountCode,
                    entryType = e.entryType,
                    amount = e.amount
                )
            }
        )

        request.subledgerEntry?.let { sub ->
            val latest = subledgerRepository.findLatestForContact(sub.contactReferenceNumber, PageRequest.of(0, 1)).firstOrNull()
            subledgerRepository.save(
                SubledgerEntryEntity(
                    groupReferenceNumber = group.referenceNumber!!,
                    contactReferenceNumber = sub.contactReferenceNumber,
                    payableAmount = sub.payableAmount,
                    receivableAmount = sub.receivableAmount,
                    runningPayable = (latest?.runningPayable ?: BigDecimal.ZERO) + sub.payableAmount,
                    runningReceivable = (latest?.runningReceivable ?: BigDecimal.ZERO) + sub.receivableAmount
                )
            )
        }
    }
}
