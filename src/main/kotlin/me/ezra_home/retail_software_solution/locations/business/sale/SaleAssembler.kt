package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleAssembler(
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val contactService: ContactService,
    private val userQualifier: UserQualifier,
    private val saleLineRepository: SaleLineRepository,
    private val salePaymentFetcher: SalePaymentFetcher
) {

    fun buildResponses(sales: List<SaleEntity>): List<SaleResponseDto> {
        val saleIds = sales.map { it.id!! }
        val allLines = saleLineRepository.findBySaleIdIn(saleIds)
        val linesBySaleId = allLines.groupBy { it.saleId }
        val contactNameMap = getContactNames()
        val productSummaries = locationProductDataFetcher.findSummaryByIds(allLines.map { it.locationProductId })
        val paymentsBySaleId = salePaymentFetcher.getPaymentsBySaleIds(saleIds)
        return sales.map { sale ->
            val lines = linesBySaleId[sale.id] ?: emptyList()
            buildResponse(sale, lines, contactNameMap, productSummaries, paymentsBySaleId[sale.id!!] ?: emptyList())
        }
    }

    private fun getContactNames():  Map<UUID, String> {
        return contactService.getAllContactDtos().associateBy(
            { it.id }, { it.identity.displayName }
        )
    }

    fun buildResponse(sale: SaleEntity, lines: List<SaleLineEntity>): SaleResponseDto {
        val contactNameMap = getContactNames()
        val productSummaries = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId })
        val payments = salePaymentFetcher.getPaymentsBySaleId(sale.id!!)
        return buildResponse(sale, lines, contactNameMap, productSummaries, payments)
    }

    private fun buildResponse(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
        contactNameMap: Map<UUID, String>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
        payments: List<SalePaymentResponseDto>
    ): SaleResponseDto {
        val lineResponses = lines.map { line ->
            val product = productSummaries.getValue(line.locationProductId)
            SaleLineResponseDto(
                id = line.id!!,
                referenceNumber = line.referenceNumber!!,
                locationProductId = line.locationProductId,
                quantity = line.quantity,
                unitId = line.unitId,
                conversionFactor = line.conversionFactor,
                unitPrice = line.unitPrice,
                lineTotal = Decimals.multiplyScale4(line.quantity, line.unitPrice),
                locationProduct = LocationProductSummaryDto(
                    id = line.locationProductId,
                    referenceNumber = product.referenceNumber,
                    productName = product.productName,
                    productGroupName = product.productGroupName,
                    baseUnitId = product.baseUnitId,
                )
            )
        }
        return SaleResponseDto(
            id = sale.id!!,
            referenceNumber = sale.referenceNumber!!,
            contactId = sale.contactId,
            walkInCustomer = sale.contactId == SystemContact.WALK_IN.id,
            contactName = contactNameMap[sale.contactId] ?: "",
            soldById = sale.soldBy,
            soldBy = userQualifier.getUserFullName(sale.soldBy),
            dateSold = sale.dateSold,
            notes = sale.notes,
            status = sale.status,
            paymentStatus = sale.paymentStatus,
            lines = lineResponses,
            payments = payments,
            saleTotal = lineResponses.sumOf { it.lineTotal }
        )
    }
}
