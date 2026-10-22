package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import me.ezra_home.retail_software_solution.util.queries.FilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext

class SalePriceFilterStrategy(private val requireDefaultSalePrice: Boolean) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        if (requireDefaultSalePrice) {
            context.whereClauses.add("${CrossTierProduct.TABLE_ALIAS}.$DEFAULT_SALE_PRICE_COLUMN IS NOT NULL")
        }
    }

    companion object {
        private const val DEFAULT_SALE_PRICE_COLUMN = "default_sale_price"

        fun matches(locationProductDto: LocationProductDto, requireDefaultSalePrice: Boolean): Boolean =
            !requireDefaultSalePrice || locationProductDto.defaultSalePrice != null
    }
}
