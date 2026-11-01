package me.ezra_home.retail_software_solution.locations.business.product_sales_report.api

import com.github.benmanes.caffeine.cache.Ticker
import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesDayBucketCache
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportFetcher
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportSnapshotBuilder
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportSnapshotCache
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportTestSummaries
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportWindow
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesSummaryAssembler
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponseWithMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ProductSalesReportSearchServiceTest {

  private val productSalesReportFetcher = mock(ProductSalesReportFetcher::class.java)
  private val productSalesSummaryAssembler = mock(ProductSalesSummaryAssembler::class.java)
  private val nanos = AtomicLong()
  private val ticker = Ticker { nanos.get() }
  private val productSalesDayBucketCache = ProductSalesDayBucketCache(ticker)
  private val productSalesReportSnapshotCache = ProductSalesReportSnapshotCache(ticker)

  private var summaries = emptyList<ProductSalesSummaryDto>()
  private val loadedWindows = mutableListOf<ProductSalesReportWindow>()

  private val firstDay = LocalDate.of(2030, 6, 1)

  @BeforeEach
  fun setSessionAndStubs() {
    SessionContextProvider.setSession(
      SessionContext(
        organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
        location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
      )
    )
    `when`(productSalesReportFetcher.fetchDailySummaries(anyWindow())).thenAnswer {
      loadedWindows.add(it.getArgument(0))
      emptyList<Nothing>()
    }
    `when`(productSalesSummaryAssembler.assemble(anyList(), anyList())).thenAnswer { summaries }
  }

  @AfterEach
  fun clearSession() {
    SessionContextProvider.clear()
  }

  // A Mockito matcher returns null, which Kotlin rejects for a non-null parameter, so a throwaway value stands in.
  private fun anyWindow(): ProductSalesReportWindow {
    any(ProductSalesReportWindow::class.java)
    return ProductSalesReportWindow(firstDay, firstDay.plusDays(1))
  }

  private fun builtAt(response: PageResponseWithMetadata<ProductSalesSummaryDto, String, ProductSalesReportSearchMetadata>) =
    response.metadata.snapshotBuiltAt

  private fun service(threshold: Long) = ProductSalesReportSearchService(
    ProductSalesReportSnapshotBuilder(productSalesReportFetcher, productSalesDayBucketCache, productSalesSummaryAssembler),
    productSalesReportSnapshotCache,
    threshold
  )

  // Distinct, strictly decreasing net sales with no losses or discounts, so the default order is p1, p2, ...
  private fun plainProducts(count: Int) = (1..count).map {
    ProductSalesReportTestSummaries.summary(
      "p$it",
      grossSales = "${1000 - it}",
      cogs = "1",
      productGroupName = if (it % 2 == 0) "Even" else "Odd"
    )
  }

  private fun request(
    previousCursor: String = "",
    requestedSize: Int = 2,
    parameters: ProductSalesReportSearchParameters = parameters()
  ) = PageRequest(previousCursor, requestedSize, parameters)

  private fun parameters(
    days: Int = 3,
    searchText: String? = null,
    sortField: ProductSalesSortField = ProductSalesSortField.NEEDS_ATTENTION,
    sortDirection: ProductSalesSortDirection = ProductSalesSortDirection.DESC,
    from: LocalDate = firstDay
  ) = ProductSalesReportSearchParameters(
    from = from,
    to = from.plusDays(days.toLong() - 1),
    searchText = searchText,
    sortField = sortField,
    sortDirection = sortDirection
  )

  private fun names(response: PageResponseWithMetadata<ProductSalesSummaryDto, String, ProductSalesReportSearchMetadata>) = response.contents.map { it.productName }

  @Test
  fun `at or below the threshold every row comes back in the requested order flagged for client-side filtering`() {
    summaries = plainProducts(3).reversed()

    val response = service(threshold = 3).search(request(parameters = parameters(searchText = "p1")))

    assertTrue(response.requireClientSideFilter)
    assertFalse(response.hasMore)
    assertEquals("", response.currentCursor)
    assertEquals(listOf("p1", "p2", "p3"), names(response))
  }

  @Test
  fun `above the threshold the service pages in memory and does not flag client-side filtering`() {
    summaries = plainProducts(5).reversed()

    val service = service(threshold = 4)
    val firstPage = service.search(request())
    val secondPage = service.search(request(previousCursor = firstPage.currentCursor))
    val thirdPage = service.search(request(previousCursor = secondPage.currentCursor))

    assertFalse(firstPage.requireClientSideFilter)
    assertEquals(listOf("p1", "p2"), names(firstPage))
    assertTrue(firstPage.hasMore)
    assertEquals(listOf("p3", "p4"), names(secondPage))
    assertTrue(secondPage.hasMore)
    assertEquals(listOf("p5"), names(thirdPage))
    assertFalse(thirdPage.hasMore)
  }

  @Test
  fun `the cached snapshot list is never reordered`() {
    summaries = plainProducts(5).reversed()
    val originalOrder = summaries.map { it.productName }

    service(threshold = 1).search(request())

    assertEquals(originalOrder, summaries.map { it.productName })
  }

  @Test
  fun `an expired snapshot is rebuilt from the day buckets without querying again`() {
    summaries = plainProducts(5).reversed()
    val service = service(threshold = 1)
    val firstPage = service.search(request())

    nanos.addAndGet(Duration.ofMinutes(2).toNanos())
    val secondPage = service.search(request(previousCursor = firstPage.currentCursor))

    assertEquals(listOf("p3", "p4"), names(secondPage))
    assertEquals(1, loadedWindows.size)
  }

  @Test
  fun `paging continues when the last row of the previous page is gone from the rebuilt snapshot`() {
    val products = plainProducts(5)
    summaries = products
    val service = service(threshold = 1)
    val firstPage = service.search(request())

    nanos.addAndGet(Duration.ofMinutes(2).toNanos())
    summaries = products.filter { it.productName != "p2" }
    val secondPage = service.search(request(previousCursor = firstPage.currentCursor))

    assertEquals(listOf("p3", "p4"), names(secondPage))
  }

  @Test
  fun `search matches product name or group case-insensitively`() {
    summaries = plainProducts(6)
    val service = service(threshold = 1)

    val byGroup = service.search(request(requestedSize = 10, parameters = parameters(searchText = "  EVEN ")))
    val byName = service.search(request(requestedSize = 10, parameters = parameters(searchText = "P3")))

    assertEquals(listOf("p2", "p4", "p6"), names(byGroup))
    assertEquals(listOf("p3"), names(byName))
  }

  @Test
  fun `search ignores case and all whitespace, in the stored name and in the search text`() {
    summaries = listOf(
      ProductSalesReportTestSummaries.summary("Rice  Bag 5kg", grossSales = "100", cogs = "10"),
      ProductSalesReportTestSummaries.summary("Beans", grossSales = "90", cogs = "10")
    )

    val response = service(threshold = 1).search(
      request(requestedSize = 10, parameters = parameters(searchText = "  RICE bag5 K g "))
    )

    assertEquals(listOf("Rice  Bag 5kg"), names(response))
  }

  @Test
  fun `the position in the order holds under a search filter`() {
    summaries = plainProducts(8)
    val service = service(threshold = 1)
    val searchParameters = parameters(searchText = "even")

    val firstPage = service.search(request(parameters = searchParameters))
    val secondPage = service.search(request(previousCursor = firstPage.currentCursor, parameters = searchParameters))

    assertEquals(listOf("p2", "p4"), names(firstPage))
    assertEquals(listOf("p6", "p8"), names(secondPage))
    assertFalse(secondPage.hasMore)
  }

  @Test
  fun `rank is the needs-attention position and never changes with the sort, a search or a page`() {
    summaries = plainProducts(8)
    val service = service(threshold = 1)
    val searchParameters = parameters(
      searchText = "even",
      sortField = ProductSalesSortField.PRODUCT_NAME,
      sortDirection = ProductSalesSortDirection.DESC
    )

    val firstPage = service.search(request(parameters = searchParameters))
    val secondPage = service.search(request(previousCursor = firstPage.currentCursor, parameters = searchParameters))

    assertEquals(listOf("p8", "p6"), names(firstPage))
    assertEquals(listOf(8, 6), firstPage.contents.map { it.rank })
    assertEquals(listOf(4, 2), secondPage.contents.map { it.rank })
  }

  @Test
  fun `a loss ranks first by needs attention`() {
    val loss = ProductSalesReportTestSummaries.summary("Loss", grossSales = "100", cogs = "150")
    summaries = plainProducts(3).reversed() + loss

    val response = service(threshold = 10).search(request())

    assertEquals(listOf("Loss", "p1", "p2", "p3"), names(response))
    assertEquals(listOf(1, 2, 3, 4), response.contents.map { it.rank })
  }

  @Test
  fun `rows page in the requested sort and direction`() {
    summaries = plainProducts(5)
    val service = service(threshold = 1)
    val ascending = parameters(sortField = ProductSalesSortField.NET_SALES, sortDirection = ProductSalesSortDirection.ASC)

    val firstPage = service.search(request(parameters = ascending))
    val secondPage = service.search(request(previousCursor = firstPage.currentCursor, parameters = ascending))
    val thirdPage = service.search(request(previousCursor = secondPage.currentCursor, parameters = ascending))

    assertEquals(listOf("p5", "p4"), names(firstPage))
    assertEquals(listOf("p3", "p2"), names(secondPage))
    assertEquals(listOf("p1"), names(thirdPage))
  }

  @Test
  fun `a null value sorts last in either direction`() {
    val noSales = ProductSalesReportTestSummaries.summary("No sales", grossSales = "0", cogs = "0")
    summaries = plainProducts(2) + noSales
    val service = service(threshold = 1)

    val ascending = service.search(
      request(
        requestedSize = 10,
        parameters = parameters(sortField = ProductSalesSortField.MARGIN_PERCENT, sortDirection = ProductSalesSortDirection.ASC)
      )
    )
    val descending = service.search(
      request(
        requestedSize = 10,
        parameters = parameters(sortField = ProductSalesSortField.MARGIN_PERCENT, sortDirection = ProductSalesSortDirection.DESC)
      )
    )

    assertEquals("No sales", names(ascending).last())
    assertEquals("No sales", names(descending).last())
  }

  @Test
  fun `a cursor only continues the sort it was issued for`() {
    summaries = plainProducts(5)
    val service = service(threshold = 1)
    val firstPage = service.search(request())

    val otherSort = parameters(sortField = ProductSalesSortField.NET_SALES, sortDirection = ProductSalesSortDirection.ASC)
    assertEquals(
      "Malformed cursor",
      assertThrows<RtsGenericException> {
        service.search(request(previousCursor = firstPage.currentCursor, parameters = otherSort))
      }.message
    )
  }

  @Test
  fun `needs attention cannot be sorted ascending`() {
    summaries = plainProducts(2)

    assertThrows<RtsGenericException> {
      service(threshold = 1).search(request(parameters = parameters(sortDirection = ProductSalesSortDirection.ASC)))
    }
  }

  @Test
  fun `a malformed cursor is rejected`() {
    summaries = plainProducts(5)

    assertEquals(
      "Malformed cursor",
      assertThrows<RtsGenericException> { service(threshold = 1).search(request(previousCursor = "garbage")) }.message
    )
  }

  @Test
  fun `both dates are required and from must not be after to`() {
    summaries = plainProducts(2)
    val service = service(threshold = 1)

    assertThrows<RtsGenericException> { service.search(request(parameters = parameters().copy(from = null))) }
    assertThrows<RtsGenericException> { service.search(request(parameters = parameters().copy(to = null))) }
    assertThrows<RtsGenericException> { service.search(request(parameters = parameters(days = 0))) }
  }

  @Test
  fun `a range is inclusive of both dates`() {
    summaries = plainProducts(2)

    service(threshold = 1).search(request(parameters = parameters(days = 1)))

    assertEquals(listOf(ProductSalesReportWindow(firstDay, firstDay.plusDays(1))), loadedWindows)
  }

  @Test
  fun `a range loads each of its days once and a later overlapping range loads only the new days`() {
    summaries = plainProducts(2)
    val service = service(threshold = 1)

    service.search(request(parameters = parameters(days = 3)))
    service.search(request(parameters = parameters(days = 3)))
    service.search(request(parameters = parameters(days = 4, from = firstDay.plusDays(1))))

    assertEquals(
      listOf(
        ProductSalesReportWindow(firstDay, firstDay.plusDays(3)),
        ProductSalesReportWindow(firstDay.plusDays(3), firstDay.plusDays(5))
      ),
      loadedWindows
    )
  }

  @Test
  fun `a day expires 15 minutes after it was loaded`() {
    summaries = plainProducts(2)
    val service = service(threshold = 1)
    service.search(request())

    nanos.addAndGet(Duration.ofMinutes(14).toNanos())
    service.search(request())
    assertEquals(1, loadedWindows.size)

    nanos.addAndGet(Duration.ofMinutes(2).toNanos())
    service.search(request())
    assertEquals(2, loadedWindows.size)
  }

  @Test
  fun `the build time is the age of the oldest day behind the range`() {
    summaries = plainProducts(5)
    val service = service(threshold = 1)
    val firstBuiltAt = builtAt(service.search(request(parameters = parameters(days = 1))))

    Thread.sleep(5)
    val widerBuiltAt = builtAt(service.search(request(parameters = parameters(days = 2))))

    assertEquals(firstBuiltAt, widerBuiltAt)
  }

  @Test
  fun `every page of one snapshot carries the same build time`() {
    summaries = plainProducts(5)
    val service = service(threshold = 1)

    val firstPage = service.search(request())
    val secondPage = service.search(request(previousCursor = firstPage.currentCursor))

    assertEquals(builtAt(firstPage), builtAt(secondPage))
  }

  @Test
  fun `page size is validated once the response is paged`() {
    summaries = plainProducts(5)
    val service = service(threshold = 1)

    assertThrows<RtsGenericException> { service.search(request(requestedSize = 0)) }
    assertThrows<RtsGenericException> { service.search(request(requestedSize = 501)) }
  }
}
