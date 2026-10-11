package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningBalanceEntity
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningBalanceHandlerForKafka
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.ContactOpeningBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ContactOpeningBalanceService(
    private val contactOpeningBalanceRepository: ContactOpeningBalanceRepository,
    private val contactService: ContactService,
    private val contactOpeningBalanceHandlerForKafka: ContactOpeningBalanceHandlerForKafka,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun set(contactId: UUID, contactOpeningBalanceSetDto: ContactOpeningBalanceSetDto) {
        if (contactOpeningBalanceSetDto.amount < BigDecimal.ZERO) {
            throw RtsGenericException("Opening balance amount must not be negative")
        }
        val postingDate = DateTimes.Local.Now.organization()
        fiscalPeriodService.requireOpenForDate(postingDate)
        val contactDto = contactService.getContactById(contactId)
        if (contactDto.status != ContactStatus.ACTIVE) {
            throw RtsGenericException("Only an active contact can take an opening balance")
        }
        orgEntityAdvisoryLock.acquire(LockNamespaces.CONTACT_OPENING_BALANCE, contactDto.referenceNumber)
        val latestRevision = contactOpeningBalanceRepository.findFirstByContactReferenceNumberAndSubledgerDirectionOrderByCreatedOnDesc(
            contactDto.referenceNumber,
            contactOpeningBalanceSetDto.subledgerDirection
        )
        val roundedAmount = Decimals.roundToScale4(contactOpeningBalanceSetDto.amount)
        val signedDelta = roundedAmount - (latestRevision?.amount ?: BigDecimal.ZERO)
        if (signedDelta.compareTo(BigDecimal.ZERO) == 0) return

        val savedContactOpeningBalanceEntity = contactOpeningBalanceRepository.save(
            ContactOpeningBalanceEntity(
                contactReferenceNumber = contactDto.referenceNumber,
                subledgerDirection = contactOpeningBalanceSetDto.subledgerDirection,
                amount = roundedAmount
            )
        )
        contactOpeningBalanceHandlerForKafka.publish(savedContactOpeningBalanceEntity, signedDelta, postingDate)
    }
}
