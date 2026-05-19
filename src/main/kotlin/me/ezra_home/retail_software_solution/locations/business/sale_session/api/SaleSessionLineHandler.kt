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
    fun addLine(sessionId: UUID, dto: SaleSessionLineAddDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        locationProductService.guardAllActive(listOf(dto.locationProductId))
        val productSummary = locationProductDataFetcher.findSummaryByIds(listOf(dto.locationProductId))
            .getValue(dto.locationProductId)
        val unitPrice = productSummary.unitPrice
            ?: throw RtsGenericException("Product ${productSummary.label} has no unit price")
        val factor = unitConversionGraphFacade.getFactor(dto.unitId, productSummary.baseUnitId)

        val newLine = SaleSessionLine(
            identity = SessionIdentity.mintFreshIdentity(),
            locationProductId = dto.locationProductId,
            productLabel = productSummary.label,
            quantity = dto.quantity,
            unitId = dto.unitId,
            conversionFactor = factor,
            unitPrice = unitPrice,
        )
        val updated = session.copy(lines = session.lines + newLine)
        return saleSessionUpdateFinalizer.finalize(updated)
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun updateLine(sessionId: UUID, dto: SaleSessionLineUpdateDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        val targetKey = dto.identity.key()
        val target = session.lines.firstOrNull { it.identity.key() == targetKey }
            ?: throw RtsGenericException("Line not found on session")
        val factor = if (target.unitId != dto.unitId) {
            val productSummary = locationProductDataFetcher.findSummaryByIds(listOf(target.locationProductId))
                .getValue(target.locationProductId)
            unitConversionGraphFacade.getFactor(dto.unitId, productSummary.baseUnitId)
        } else {
            target.conversionFactor
        }
        val updated = session.copy(
            lines = session.lines.map { line ->
                if (line.identity.key() == targetKey) {
                    line.copy(quantity = dto.quantity, unitId = dto.unitId, conversionFactor = factor)
                } else line
            }
        )
        return saleSessionUpdateFinalizer.finalize(updated)
    }

    fun removeLine(sessionId: UUID, dto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        saleSessionValidator.guardMutable(session)
        val targetKey = dto.identity.key()
        val survivingLines = session.lines.filter { it.identity.key() != targetKey }
        if (survivingLines.size == session.lines.size) {
            throw RtsGenericException("Line not found on session")
        }
        val survivingAdjustments = session.adjustments.filter { adj ->
            adj.lineIdentity == null || adj.lineIdentity.key() != targetKey
        }
        val updated = session.copy(lines = survivingLines, adjustments = survivingAdjustments)
        return saleSessionUpdateFinalizer.finalize(updated)
    }

}
