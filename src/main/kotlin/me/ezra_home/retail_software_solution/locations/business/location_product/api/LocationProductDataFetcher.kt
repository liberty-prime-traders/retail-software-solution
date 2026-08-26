package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEnricher
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductForPurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductForSaleAssembler
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductPagedSearch
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
    private val locationProductPagedSearch: LocationProductPagedSearch,
    private val locationProductEnricher: LocationProductEnricher,
    private val locationProductForSaleAssembler: LocationProductForSaleAssembler,
    ) {

    fun searchWithParameters(
        pageRequest: PageRequest<ProductSearchParameters, String>
    ): PageResponse<LocationProductResponseDto, String> {
        val page = locationProductPagedSearch.searchWithParameters(pageRequest)
        return PageResponse(
            currentCursor = page.currentCursor,
            hasMore = page.hasMore,
            contents = locationProductEnricher.convertToResponseDto(page.contents.toList()),
            requireClientSideFilter = page.requireClientSideFilter,
        )
    }

    fun searchForSale(
        pageRequest: PageRequest<LocationProductSearchParameters, String>
    ): PageResponse<LocationProductWithAvailability, String> {
        val page = locationProductPagedSearch.searchWithParameters(toActiveProductSearch(pageRequest))
        return PageResponse(
            currentCursor = page.currentCursor,
            hasMore = page.hasMore,
            contents = locationProductForSaleAssembler.assemble(page.contents.toList()),
            requireClientSideFilter = page.requireClientSideFilter,
        )
    }

    fun searchForPurchase(
        pageRequest: PageRequest<LocationProductSearchParameters, String>
    ): PageResponse<LocationProductForPurchaseDto, String> {
        val page = locationProductPagedSearch.searchWithParameters(toActiveProductSearch(pageRequest))
        return PageResponse(
            currentCursor = page.currentCursor,
            hasMore = page.hasMore,
            contents = LocationProductForPurchaseAssembler.assemble(page.contents.toList()),
            requireClientSideFilter = page.requireClientSideFilter,
        )
    }

    fun generateFormattedQuery(pageRequest: PageRequest<ProductSearchParameters, String>): String {
        return locationProductPagedSearch.generateFormattedQuery(pageRequest)
    }

    private fun toActiveProductSearch(
        pageRequest: PageRequest<LocationProductSearchParameters, String>
    ): PageRequest<ProductSearchParameters, String> = PageRequest(
        previousCursor = pageRequest.previousCursor,
        requestedSize = pageRequest.requestedSize,
        parameters = ProductSearchParameters(
            searchText = pageRequest.parameters.searchText,
            excludeIds = pageRequest.parameters.excludeIds,
            statusList = setOf(ProductStatus.ACTIVE),
            searchStrategy = pageRequest.parameters.searchStrategy
        )
    )

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

    fun getOrgProductIds(locationProductIds: Collection<UUID>): Map<UUID, UUID> =
        locationProductRepository.findAllById(locationProductIds)
            .associate { it.id!! to it.orgProductId }

    fun findIdentityByOrgProductId(orgProductId: UUID): LocationProductIdentityDto {
        val entity = locationProductRepository.findByOrgProductId(orgProductId)
            ?: throw RtsGenericException("No location product found for product $orgProductId")
        return LocationProductIdentityDto(locationProductId = entity.id!!, orgProductId = entity.orgProductId)
    }

    fun findIdentitiesByOrgProductIds(orgProductIds: Collection<UUID>): Map<UUID, LocationProductIdentityDto> =
        locationProductRepository.findByOrgProductIdIn(orgProductIds)
            .associate { entity ->
                entity.orgProductId to LocationProductIdentityDto(
                    locationProductId = entity.id!!,
                    orgProductId = entity.orgProductId
                )
            }

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
