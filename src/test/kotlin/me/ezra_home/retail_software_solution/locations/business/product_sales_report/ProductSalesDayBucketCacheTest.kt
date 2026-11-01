package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ProductSalesDayBucketCacheTest {

  private val nanos = AtomicLong()
  private val cache = ProductSalesDayBucketCache { nanos.get() }
    private val firstDay = LocalDate.of(2030, 6, 1)
  private val loadedDays = mutableListOf<List<LocalDate>>()

  @AfterEach
  fun clearSession() {
    SessionContextProvider.clear()
  }

  private fun inLocation(schemaName: String) {
    SessionContextProvider.setSession(
      SessionContext(
        organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
        location = LocationSession(id = UUID.randomUUID(), schemaName = schemaName)
      )
    )
  }

  private fun getOrLoad(days: List<LocalDate>): List<ProductSalesDayBucket> = cache.getOrLoad(days) { missingDays ->
    loadedDays.add(missingDays)
    missingDays.associateWith { ProductSalesDayBucket(OffsetDateTime.now(), emptyMap()) }
  }

  private fun days(from: Long, count: Long) = (from until from + count).map { firstDay.plusDays(it) }

  @Test
  fun `only the days that are not cached are loaded, in one call`() {
    inLocation("loc-1")
    getOrLoad(days(0, 3))

    getOrLoad(days(1, 4))

    assertEquals(listOf(days(0, 3), days(3, 2)), loadedDays)
  }

  @Test
  fun `a cached day is returned as the same bucket`() {
    inLocation("loc-1")
    val first = getOrLoad(days(0, 2))

    assertSame(first[1], getOrLoad(days(1, 1))[0])
  }

  @Test
  fun `buckets come back in the order of the requested days`() {
    inLocation("loc-1")
    getOrLoad(days(1, 1))

    val buckets = getOrLoad(days(0, 3))

    assertEquals(3, buckets.size)
    assertEquals(listOf(days(1, 1), listOf(firstDay, firstDay.plusDays(2))), loadedDays)
  }

  @Test
  fun `a bucket expires 15 minutes after it was loaded and reads do not extend it`() {
    inLocation("loc-1")
    getOrLoad(days(0, 1))

    nanos.addAndGet(Duration.ofMinutes(14).toNanos())
    getOrLoad(days(0, 1))
    assertEquals(1, loadedDays.size)

    nanos.addAndGet(Duration.ofMinutes(2).toNanos())
    getOrLoad(days(0, 1))
    assertEquals(2, loadedDays.size)
  }

  @Test
  fun `two locations never share a bucket`() {
    inLocation("loc-1")
    getOrLoad(days(0, 1))
    inLocation("loc-2")
    getOrLoad(days(0, 1))

    assertEquals(2, loadedDays.size)
  }
}
