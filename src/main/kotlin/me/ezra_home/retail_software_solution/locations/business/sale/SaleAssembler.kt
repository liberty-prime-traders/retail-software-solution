package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SaleAssembler(
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val contactService: ContactService,
    private val userQualifier: UserQualifier,
    private val saleLineRepository: SaleLineRepository,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val saleAdjustmentFetcher: SaleAdjustmentFetcher
) {

    fun buildResponses(sales: List<SaleEntity>): List<SaleResponseDto> {
        val saleIds = sales.map { it.id!! }
        val allLines = saleLineRepository.findBySaleIdIn(saleIds)
        val linesBySaleId = allLines.groupBy { it.saleId }
        val contactNameMap = getContactNames()
        val productSummaries = locationProductDataFetcher.findSummaryByIds(allLines.map { it.locationProductId })
        val paymentsBySaleId = salePaymentFetcher.getPaymentsBySaleIds(saleIds)
        val paidAmountBySaleId = salePaymentFetcher.calculatePaidAmounts(saleIds)
        val adjustmentsBySaleId = saleAdjustmentFetcher.getAdjustmentsBySaleIds(saleIds)
        return sales.map { sale ->
            val lines = linesBySaleId[sale.id] ?: emptyList()
            val adjustments = adjustmentsBySaleId[sale.id!!] ?: emptyList()
            val totalPaid = paidAmountBySaleId[sale.id!!] ?: BigDecimal.ZERO
            buildResponse(sale, lines, contactNameMap, productSummaries, paymentsBySaleId[sale.id!!] ?: emptyList(), adjustments, totalPaid)
        }
    }

    private fun getContactNames(): Map<UUID, String> {
        return contactService.getAllContactDtos().associateBy(
            { it.id }, { it.identity.displayName }
        )
    }

    fun buildResponse(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ): SaleResponseDto {
        val contactNameMap = getContactNames()
        val payments = salePaymentFetcher.getPaymentsBySaleId(sale.id!!)
        val adjustments = saleAdjustmentFetcher.getAdjustmentsBySaleId(sale.id!!)
        val totalPaid = salePaymentFetcher.calculatePaidAmount(sale.id!!)
        return buildResponse(sale, lines, contactNameMap, productSummaries, payments, adjustments, totalPaid)
    }

    private fun buildResponse(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
        contactNameMap: Map<UUID, String>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
        payments: List<SalePaymentResponseDto>,
        adjustments: List<SaleAdjustmentResponseDto>,
        totalPaid: BigDecimal
    ): SaleResponseDto {
        val lineResponses = SaleLineMapper.toResponseLines(lines, productSummaries)
        return SaleResponseDto(
            id = sale.id!!,
            referenceNumber = sale.requiredReference(),
            contactId = sale.contactId,
            walkInCustomer = sale.contactId == SystemContact.WALK_IN.id,
            contactName = contactNameMap[sale.contactId] ?: "",
            soldById = sale.soldById,
            soldBy = userQualifier.getUserFullName(sale.soldById),
            dateSold = sale.dateSold,
            notes = sale.notes,
            status = sale.status,
            paymentStatus = sale.paymentStatus,
            lines = lineResponses,
            adjustments = adjustments,
            payments = payments,
            subtotal = sale.subtotal,
            lineLevelDiscountTotal = sale.lineLevelDiscountTotal,
            orderLevelDiscountTotal = sale.orderLevelDiscountTotal,
            lineLevelSurchargeTotal = sale.lineLevelSurchargeTotal,
            orderLevelSurchargeTotal = sale.orderLevelSurchargeTotal,
            taxTotal = sale.taxTotal,
            grandTotal = sale.grandTotal,
            totalPaid = totalPaid
        )
    }
}
