package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductSearchService
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductDataFetcher(
    private val locationProductRepository: LocationProductRepository,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val locationProductSearchService: LocationProductSearchService,
    ) {

    fun searchWithParameters(
        pageRequest: PageRequest<ProductSearchParameters, String>
    ): PageResponse<LocationProductResponseDto, String> {
        return locationProductSearchService.searchWithParameters(pageRequest)
    }

    fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
        return locationProductSearchService.generateFormattedQuery(pageRequest)
    }

    fun findSummaryByIds(ids: Collection<UUID>): Map<UUID, LocationProductSummaryDto> =
        locationProductRepository.findAllById(ids).associate { entity ->
            entity.id!! to LocationProductSummaryDto(
                id = entity.id!!,
                referenceNumber = entity.requiredReference(),
                productName = entity.productName,
                productGroupName = entity.productGroupName,
                baseUnitId = entity.baseUnitId,
            )
        }

    fun getDefaultSalePrices(locationProductIds: Collection<UUID>): Map<UUID, BigDecimal?> =
        locationProductRepository.findAllById(locationProductIds)
            .associate { it.id!! to it.defaultSalePrice }

    fun getBaseUnitIds(locationProductIds: Collection<UUID>): Map<UUID, UUID> =
        locationProductRepository.findAllById(locationProductIds)
            .associate { it.id!! to it.baseUnitId }

    fun getConversionFactors(productUnitRequests: List<LocationProductUnitRequestDto>): Map<UUID, BigDecimal> {
        if (productUnitRequests.isEmpty()) return emptyMap()
        return populateConversionInfo(productUnitRequests).associate { it.locationProductId to it.conversionFactor }
    }

    private fun populateConversionInfo(productUnitRequests: List<LocationProductUnitRequestDto>): List<LocationProductUnitDto> {
        if (productUnitRequests.isEmpty()) return emptyList()
        val baseUnitIds = getBaseUnitIds(productUnitRequests.map { it.locationProductId })
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return productUnitRequests.map { (locationProductId, unitId) ->
            val baseUnitId = baseUnitIds[locationProductId]!!
            LocationProductUnitDto(
                locationProductId = locationProductId,
                unitId = unitId,
                baseUnitId = baseUnitId,
                conversionFactor = unitConversionGraph.getFactor(unitId, baseUnitId)
            )
        }
    }
}
