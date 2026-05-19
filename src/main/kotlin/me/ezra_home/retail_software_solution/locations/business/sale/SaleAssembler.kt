package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSummary
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleAssembler(
    private val contactService: ContactService,
    private val userQualifier: UserQualifier,
    private val salePaymentFetcher: SalePaymentFetcher,
) {

    fun buildSummaries(saleEntities: List<SaleEntity>): List<SaleSummary> {
        val saleIds = saleEntities.map { it.id!! }
        val contactNameMap = getContactNames()
        val paidAmountBySaleId = salePaymentFetcher.calculatePaidAmounts(saleIds)
        return saleEntities.map { saleEntity ->
            val totalPaid = paidAmountBySaleId[saleEntity.id!!] ?: BigDecimal.ZERO
            buildSummary(saleEntity, contactNameMap, totalPaid)
        }
    }

    private fun getContactNames(): Map<UUID, String> {
        return contactService.getAllContactDtos().associateBy(
            { it.id }, { it.identity.displayName }
        )
    }

    fun buildSummary(saleEntity: SaleEntity): SaleSummary {
        val contactNameMap = getContactNames()
        val totalPaid = salePaymentFetcher.calculatePaidAmount(saleEntity.id!!)
        return buildSummary(saleEntity, contactNameMap, totalPaid)
    }

    private fun buildSummary(
        saleEntity: SaleEntity,
        contactNameMap: Map<UUID, String>,
        totalPaid: BigDecimal
    ): SaleSummary {
        return SaleSummary(
            id = saleEntity.id!!,
            referenceNumber = saleEntity.requiredReference(),
            contactName = contactNameMap[saleEntity.contactId] ?: "",
            soldBy = userQualifier.getUserFullName(saleEntity.soldById),
            dateSold = saleEntity.dateSold,
            status = saleEntity.status,
            paymentStatus = saleEntity.paymentStatus,
            subtotal = saleEntity.subtotal,
            grandTotal = saleEntity.grandTotal,
            totalPaid = totalPaid
        )
    }
}
