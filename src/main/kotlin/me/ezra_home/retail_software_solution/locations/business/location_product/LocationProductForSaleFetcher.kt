package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForSaleDto
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductForSaleFetcher(
    locationProductForSaleSearchExecutor: LocationProductForSaleSearchExecutor
) : LocationProductFetcherBase<LocationProductForSaleDto>(locationProductForSaleSearchExecutor) {

    override fun cursorFrom(row: LocationProductForSaleDto): String = row.productName
}
