package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SalePriceFilterStrategyTest {

    private fun locationProductDto(productName: String, defaultSalePrice: BigDecimal?, status: ProductStatus = ProductStatus.ACTIVE) =
        LocationProductDto(
            id = UUID.randomUUID(),
            createdById = UUID.randomUUID(),
            createdOn = OffsetDateTime.now(),
            referenceNumber = productName,
            orgProductId = UUID.randomUUID(),
            productName = productName,
            productGroupName = "G",
            categoryId = UUID.randomUUID(),
            baseUnitId = UUID.randomUUID(),
            defaultSalePrice = defaultSalePrice,
            status = status,
        )

    @Test
    fun `required price adds an IS NOT NULL clause on the price column`() {
        val queryBuilderContext = QueryBuilderContext()

        SalePriceFilterStrategy(requireDefaultSalePrice = true).apply(queryBuilderContext)

        assertEquals(listOf("p.default_sale_price IS NOT NULL"), queryBuilderContext.whereClauses)
    }

    @Test
    fun `price not required adds no clause`() {
        val queryBuilderContext = QueryBuilderContext()

        SalePriceFilterStrategy(requireDefaultSalePrice = false).apply(queryBuilderContext)

        assertTrue(queryBuilderContext.whereClauses.isEmpty())
    }

    @Test
    fun `matches rejects an unpriced product only when a price is required`() {
        val unpricedProduct = locationProductDto("A", defaultSalePrice = null)
        val pricedProduct = locationProductDto("B", defaultSalePrice = BigDecimal("2.50"))

        assertFalse(SalePriceFilterStrategy.matches(unpricedProduct, requireDefaultSalePrice = true))
        assertTrue(SalePriceFilterStrategy.matches(unpricedProduct, requireDefaultSalePrice = false))
        assertTrue(SalePriceFilterStrategy.matches(pricedProduct, requireDefaultSalePrice = true))
    }

    @Test
    fun `client-side search path returns only priced active products regardless of how many unpriced ones precede them`() {
        val unpricedProducts = (1..15).map { locationProductDto("A$it", defaultSalePrice = null) }
        val pricedProducts = (1..3).map { locationProductDto("Z$it", defaultSalePrice = BigDecimal.TEN) }
        val discontinuedPricedProduct = locationProductDto("Y", BigDecimal.TEN, ProductStatus.DISCONTINUED)
        val locationProductCache = mock(LocationProductCache::class.java)
        `when`(locationProductCache.countAllLocationProducts()).thenReturn(19L)
        `when`(locationProductCache.findAllLocationProducts())
            .thenReturn(unpricedProducts + discontinuedPricedProduct + pricedProducts)
        val locationProductPagedSearch = LocationProductPagedSearch(
            locationProductCache,
            mock(LocationProductDtoFetcher::class.java),
            clientSideFilterThreshold = Int.MAX_VALUE,
        )

        val searchPage = locationProductPagedSearch.searchWithParameters(
            PageRequest(
                previousCursor = "",
                requestedSize = 10,
                parameters = ProductSearchParameters(requireDefaultSalePrice = true),
            )
        )

        assertEquals(pricedProducts, searchPage.contents)
    }
}
