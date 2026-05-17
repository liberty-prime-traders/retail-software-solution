package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductFetcher(
    locationProductSearchExecutor: LocationProductSearchExecutor
) : LocationProductFetcherBase<LocationProductResponseDto>(locationProductSearchExecutor) {

    override fun cursorFrom(row: LocationProductResponseDto): String = row.productName
}
