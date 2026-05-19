package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleResponseDto
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

    fun buildResponses(sales: List<SaleEntity>): List<SaleResponseDto> {
        val saleIds = sales.map { it.id!! }
        val contactNameMap = getContactNames()
        val paidAmountBySaleId = salePaymentFetcher.calculatePaidAmounts(saleIds)
        return sales.map { sale ->
            val totalPaid = paidAmountBySaleId[sale.id!!] ?: BigDecimal.ZERO
            buildResponse(sale, contactNameMap, totalPaid)
        }
    }

    private fun getContactNames(): Map<UUID, String> {
        return contactService.getAllContactDtos().associateBy(
            { it.id }, { it.identity.displayName }
        )
    }

    fun buildResponse(sale: SaleEntity): SaleResponseDto {
        val contactNameMap = getContactNames()
        val totalPaid = salePaymentFetcher.calculatePaidAmount(sale.id!!)
        return buildResponse(sale, contactNameMap, totalPaid)
    }

    private fun buildResponse(
        sale: SaleEntity,
        contactNameMap: Map<UUID, String>,
        totalPaid: BigDecimal
    ): SaleResponseDto {
        return SaleResponseDto(
            id = sale.id!!,
            referenceNumber = sale.requiredReference(),
            contactName = contactNameMap[sale.contactId] ?: "",
            soldBy = userQualifier.getUserFullName(sale.soldById),
            dateSold = sale.dateSold,
            status = sale.status,
            paymentStatus = sale.paymentStatus,
            subtotal = sale.subtotal,
            grandTotal = sale.grandTotal,
            totalPaid = totalPaid
        )
    }
}
