package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class StockTransferSummaryService(
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val locationService: LocationService
) {

    fun fetchByLocation(limit: Int): List<StockTransferSummaryDto> {
        val orders = stockTransferOrderDataFetcher.getTopByLocationId(limit)
        return toSummaries(orders)
    }

    fun fetchForOrganization(limit: Int): List<StockTransferSummaryDto> {
        val orders = stockTransferOrderDataFetcher.getTopForOrganization(limit)
        return toSummaries(orders)
    }

    private fun toSummaries(orders: List<StockTransferOrderDomainDto>): List<StockTransferSummaryDto> {
        val locationNameById = locationService.getAllLocationDtos()
            .associate { it.id to it.name }
        return orders.map { order ->
            order.toSummaryDto(
                sourceLocationName = locationNameById[order.sourceLocationId],
                destinationLocationName = locationNameById[order.destinationLocationId]
            )
        }
    }
}
