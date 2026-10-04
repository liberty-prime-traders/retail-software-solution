package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSummary
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.util.business.mappers.NameResolution
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleAssembler(
    private val nameResolution: NameResolution,
    private val userQualifier: UserQualifier,
    private val salePaymentFetcher: SalePaymentFetcher,
) {

    fun buildSummaries(saleEntities: List<SaleEntity>): List<SaleSummary> {
        val saleIds = saleEntities.map { it.id!! }
        val contactNameMap = nameResolution.organizationContacts(saleEntities.map { it.contactId })
        val paidAmountBySaleId = salePaymentFetcher.calculatePaidAmounts(saleIds)
        return saleEntities.map { saleEntity ->
            val totalPaid = paidAmountBySaleId[saleEntity.id!!] ?: BigDecimal.ZERO
            buildSummary(saleEntity, contactNameMap, totalPaid)
        }
    }

    fun buildSummary(saleEntity: SaleEntity): SaleSummary {
        val contactNameMap = nameResolution.organizationContacts(listOf(saleEntity.contactId))
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
            contactName = contactNameMap.getValue(saleEntity.contactId),
            soldBy = userQualifier.getUserFullName(saleEntity.soldById),
            dateSold = saleEntity.dateSold,
            status = saleEntity.status,
            paymentStatus = saleEntity.paymentStatus,
            arrearsTotal = SaleArrears.arrearsTotal(saleEntity.status, saleEntity.receivableTotal(), totalPaid),
            totalPaid = totalPaid
        )
    }
}
