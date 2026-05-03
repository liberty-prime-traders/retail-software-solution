package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleLinePreparer(
    private val locationProductService: LocationProductService,
    private val saleValidator: SaleValidator,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val locationProductDataFetcher: LocationProductDataFetcher
) {

    fun prepareForCreate(dto: SaleCreateDto): Map<UUID, BigDecimal> {
        SaleValidator.guardHasLines(dto.linesToAdd)
        val resolvedQuantities = prepareForInsert(dto.linesToAdd)
        if (dto.walkInCustomer) {
            val saleTotal = dto.linesToAdd.sumOf { Decimals.multiplyScale4(it.quantity, it.unitPrice) }
            if (dto.payments.sumOf { it.amount } < saleTotal) {
                throw RtsGenericException("Walk-in sales require full payment coverage")
            }
        }
        return resolvedQuantities
    }

    fun prepareForInsert(dtoLines: List<SaleLineCreateDto>): Map<UUID, BigDecimal> {
        val locationProductIds = dtoLines.map { it.locationProductId }
        SaleValidator.guardNoDuplicateProducts(locationProductIds)
        locationProductService.guardAllActive(locationProductIds)
        val productSummaries = locationProductDataFetcher.findSummaryByIds(locationProductIds)
        val resolvedQuantities = dtoLines.associate { line ->
            line.locationProductId to unitConversionGraphFacade.convert(
                line.unitId, productSummaries[line.locationProductId]!!.baseUnitId, line.quantity
            )
        }
        saleValidator.ensureSufficientStockForLines(resolvedQuantities, productSummaries)
        return resolvedQuantities
    }

    fun resolveFactors(dtoLines: List<SaleLineCreateDto>): Map<UUID, BigDecimal> {
        val baseUnitsByLocationProductId = locationProductDataFetcher.getBaseUnitIds(dtoLines.map { it.locationProductId })
        val graph = unitConversionGraphFacade.getOrLoad()
        return dtoLines.associate { line ->
            val baseUnitId = baseUnitsByLocationProductId[line.locationProductId]!!
            line.locationProductId to (graph[line.unitId]?.get(baseUnitId)?.factor!!)
        }
    }
}
