package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleLinesInsertPreparer(
    private val locationProductService: LocationProductService,
    private val saleValidator: SaleValidator,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val locationProductDataFetcher: LocationProductDataFetcher,
) {

    fun prepareForSaleConfirmation(dto: SaleCreateDto): SaleLinesInsertContext {
        SaleValidator.guardHasLines(dto.linesToAdd)
        return prepareForSaleCreation(dto.linesToAdd)
    }

    fun prepareForSaleCreation(saleLinesForCreate: List<SaleLineCreateDto>): SaleLinesInsertContext {
        if (saleLinesForCreate.isEmpty()) return SaleLinesInsertContext(emptyMap(), emptyMap())
        SaleValidator.guardPositiveLineQuantities(saleLinesForCreate)
        val locationProductIds = saleLinesForCreate.map { it.locationProductId }
        SaleValidator.guardNoDuplicateProducts(locationProductIds)
        locationProductService.guardAllActive(locationProductIds)
        val productSummaries = locationProductDataFetcher.findSummaryByIds(locationProductIds)
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val factorByProductId = mutableMapOf<UUID, BigDecimal>()

        val resolvedBaseQuantitiesPerProduct = saleLinesForCreate.associate { line ->
            val targetUnitId = productSummaries.getValue(line.locationProductId).baseUnitId
            val target = unitConversionGraph.getTarget(line.unitId, targetUnitId)
            factorByProductId[line.locationProductId] = target.factor
            line.locationProductId to target.applyTo(line.quantity)
        }
        saleValidator.ensureSufficientStockForLines(resolvedBaseQuantitiesPerProduct, productSummaries)
        return SaleLinesInsertContext(productSummaries, factorByProductId)
    }
}
