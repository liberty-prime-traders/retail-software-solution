package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForPurchaseDto
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductForPurchaseFetcher(
    locationProductForPurchaseSearchExecutor: LocationProductForPurchaseSearchExecutor
) : LocationProductFetcherBase<LocationProductForPurchaseDto>(locationProductForPurchaseSearchExecutor) {

    override fun cursorFrom(row: LocationProductForPurchaseDto): String = row.productName
}
