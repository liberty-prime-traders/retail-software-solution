package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class SaleSessionLineHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun addLine(sessionId: String, dto: SaleSessionLineAddDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        locationProductService.guardAllActive(listOf(dto.locationProductId))
        val productSummary = locationProductDataFetcher.findSummaryByIds(listOf(dto.locationProductId))
            .getValue(dto.locationProductId)
        val unitPrice = productSummary.unitPrice
            ?: throw RtsGenericException("Product ${productSummary.label} has no unit price")
        val factor = unitConversionGraphFacade.getFactor(dto.unitId, productSummary.baseUnitId)

        val newLine = SaleSessionLine(
            id = SessionIdentity.fresh(),
            locationProductId = dto.locationProductId,
            productLabel = productSummary.label,
            quantity = dto.quantity,
            unitId = dto.unitId,
            conversionFactor = factor,
            unitPrice = unitPrice,
        )
        val updated = session.copy(lines = session.lines + newLine)
        return finishMutation(updated)
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun updateLine(sessionId: String, dto: SaleSessionLineUpdateDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val targetKey = dto.id.key()
        val target = session.lines.firstOrNull { it.id.key() == targetKey }
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
                if (line.id.key() == targetKey) {
                    line.copy(quantity = dto.quantity, unitId = dto.unitId, conversionFactor = factor)
                } else line
            }
        )
        return finishMutation(updated)
    }

    fun removeLine(sessionId: String, dto: SaleSessionRowIdentityDto): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val targetKey = dto.id.key()
        val survivingLines = session.lines.filter { it.id.key() != targetKey }
        if (survivingLines.size == session.lines.size) {
            throw RtsGenericException("Line not found on session")
        }
        val survivingAdjustments = session.adjustments.filter { adj ->
            adj.lineId == null || adj.lineId.key() != targetKey
        }
        val updated = session.copy(lines = survivingLines, adjustments = survivingAdjustments)
        return finishMutation(updated)
    }

    private fun finishMutation(updated: SaleSession): SaleSessionResponseDto {
        val now = DateTimes.Offset.Now.organization()
        val touched = updated.touched(SessionContextProvider.getUserId(), now)
        val withTotals = saleSessionTotalsCalculator.recompute(touched)
        saleSessionValidator.validate(withTotals)
        saleSessionStore.save(withTotals)
        return saleSessionAssembler.buildResponse(withTotals)
    }
}
