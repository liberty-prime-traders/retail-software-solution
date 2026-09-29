package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.queries.HasSearchStrategy
import me.ezra_home.retail_software_solution.util.queries.SearchStrategy
import java.util.UUID

data class LocationProductSearchParameters(
    val searchText: String? = null,
    val excludeIds: Set<UUID> = emptySet(),
    override val searchStrategy: SearchStrategy = SearchStrategy.NONE
) : HasSearchStrategy<LocationProductSearchParameters> {

    override fun copySelf(searchStrategy: SearchStrategy): LocationProductSearchParameters =
        copy(searchStrategy = searchStrategy)
}
