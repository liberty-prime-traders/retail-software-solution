package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.util.UUID

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionLineHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun addLine(sessionId: UUID, lineAddDto: SaleSessionLineAddDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        locationProductService.guardAllActive(listOf(lineAddDto.locationProductId))
        val productSummary = locationProductDataFetcher.findSummaryByIds(listOf(lineAddDto.locationProductId))
            .getValue(lineAddDto.locationProductId)
        val unitPrice = productSummary.unitPrice
            ?: throw RtsGenericException("Product ${productSummary.label} has no unit price")
        val conversionFactor = unitConversionGraphFacade.getFactor(lineAddDto.unitId, productSummary.baseUnitId)

        val newSaleSessionLine = SaleSessionLine(
            identity = SessionIdentity.mintFreshIdentity(),
            locationProductId = lineAddDto.locationProductId,
            productLabel = productSummary.label,
            quantity = lineAddDto.quantity,
            unitId = lineAddDto.unitId,
            conversionFactor = conversionFactor,
            unitPrice = unitPrice,
        )
        val updatedSaleSession = saleSession.copy(saleLines = saleSession.saleLines + newSaleSessionLine)
        return saleSessionUpdateFinalizer.finalize(updatedSaleSession)
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun updateLine(sessionId: UUID, lineUpdateDto: SaleSessionLineUpdateDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        val targetLineKey = lineUpdateDto.identity.key()
        val targetSaleSessionLine = saleSession.saleLines.firstOrNull { it.identity.key() == targetLineKey }
            ?: throw RtsGenericException("Line not found on session")
        val conversionFactor = if (targetSaleSessionLine.unitId != lineUpdateDto.unitId) {
            val productSummary = locationProductDataFetcher.findSummaryByIds(listOf(targetSaleSessionLine.locationProductId))
                .getValue(targetSaleSessionLine.locationProductId)
            unitConversionGraphFacade.getFactor(lineUpdateDto.unitId, productSummary.baseUnitId)
        } else {
            targetSaleSessionLine.conversionFactor
        }
        val updatedSaleSession = saleSession.copy(
            saleLines = saleSession.saleLines.map { saleSessionLine ->
                if (saleSessionLine.identity.key() == targetLineKey) {
                    saleSessionLine.copy(
                        quantity = lineUpdateDto.quantity,
                        unitId = lineUpdateDto.unitId,
                        conversionFactor = conversionFactor,
                    )
                } else saleSessionLine
            }
        )
        return saleSessionUpdateFinalizer.finalize(updatedSaleSession)
    }

    fun removeLine(sessionId: UUID, rowIdentityDto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val saleSession = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(saleSession)
        val targetLineKey = rowIdentityDto.identity.key()
        val survivingSaleLines = saleSession.saleLines.filter { it.identity.key() != targetLineKey }
        if (survivingSaleLines.size == saleSession.saleLines.size) {
            throw RtsGenericException("Line not found on session")
        }
        val survivingSaleAdjustments = saleSession.saleAdjustments.filter { saleSessionAdjustment ->
            saleSessionAdjustment.relatedSaleLineIdentity == null ||
                saleSessionAdjustment.relatedSaleLineIdentity.key() != targetLineKey
        }
        val updatedSaleSession = saleSession.copy(saleLines = survivingSaleLines, saleAdjustments = survivingSaleAdjustments)
        return saleSessionUpdateFinalizer.finalize(updatedSaleSession)
    }

}
