package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineAddDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLineUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionLineChangeContext(
    val locationProductSummariesById: Map<UUID, LocationProductSummaryDto>,
    val defaultSalePricesByLocationProductId: Map<UUID, BigDecimal?>,
    val newConversionRatiosByLineKey: Map<UUID, ConversionRatio>,
)

@Service
class SaleSessionLineChangeContextBuilder(
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade
) {

    fun buildContext(
        additions: List<SaleSessionLineAddDto>,
        updates: List<SaleSessionLineUpdateDto>,
        saleSessionLinesByKey: Map<UUID, SaleSessionLine>,
    ): SaleSessionLineChangeContext {

        val locationProductIdsWithUnitChange = updates
            .mapNotNull { lineUpdateDto ->
                val saleSessionLine = saleSessionLinesByKey.getValue(lineUpdateDto.identity.key())
                if (saleSessionLine.unitId != lineUpdateDto.unitId) saleSessionLine.locationProductId else null
            }
            .distinct()

        val additionLocationProductIds = additions.map { it.locationProductId }.distinct()
        if (additionLocationProductIds.isNotEmpty()) {
            locationProductService.guardAllActive(additionLocationProductIds)
        }

        val locationProductSummariesById = (additionLocationProductIds + locationProductIdsWithUnitChange)
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { locationProductDataFetcher.findSummaryByIds(it) }
            ?: emptyMap()

        val newConversionRatiosByLineKey = resolveNewConversionRatios(
            updates = updates,
            saleSessionLinesByKey = saleSessionLinesByKey,
            locationProductSummariesById = locationProductSummariesById,
        )

        val defaultSalePricesByLocationProductId = additionLocationProductIds
            .takeIf { it.isNotEmpty() }
            ?.let { locationProductDataFetcher.getDefaultSalePrices(it) }
            ?: emptyMap()

        return SaleSessionLineChangeContext(
            locationProductSummariesById = locationProductSummariesById,
            defaultSalePricesByLocationProductId = defaultSalePricesByLocationProductId,
            newConversionRatiosByLineKey = newConversionRatiosByLineKey,
        )
    }

    private fun resolveNewConversionRatios(
        updates: List<SaleSessionLineUpdateDto>,
        saleSessionLinesByKey: Map<UUID, SaleSessionLine>,
        locationProductSummariesById: Map<UUID, LocationProductSummaryDto>,
    ): Map<UUID, ConversionRatio> {
        val unitChangingUpdates = updates.filter { updatedSaleLine ->
            saleSessionLinesByKey.getValue(updatedSaleLine.identity.key()).unitId != updatedSaleLine.unitId
        }
        if (unitChangingUpdates.isEmpty()) return emptyMap()
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return unitChangingUpdates.associate { updatedSaleLine ->
            val saleSessionLine = saleSessionLinesByKey.getValue(updatedSaleLine.identity.key())
            val baseUnitId = locationProductSummariesById.getValue(saleSessionLine.locationProductId).baseUnitId
            updatedSaleLine.identity.key() to unitConversionGraph.getRatio(updatedSaleLine.unitId, baseUnitId)
        }
    }
}
