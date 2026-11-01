package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import com.github.benmanes.caffeine.cache.Ticker
import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ProductSalesReportSnapshotCacheTest {

  private val nanos = AtomicLong()
  private val cache = ProductSalesReportSnapshotCache(Ticker { nanos.get() })
  private val window = ProductSalesReportWindow(LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 10))
  private var buildCount = 0

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

  private fun snapshot() = ProductSalesReportSnapshot(OffsetDateTime.now(), emptyList())

  private fun getOrBuild(): ProductSalesReportSnapshot = cache.getOrBuild(window) {
    buildCount++
    snapshot()
  }

  private fun advance(duration: Duration) {
    nanos.addAndGet(duration.toNanos())
  }

  @Test
  fun `a second read inside the time to live reuses the snapshot`() {
    inLocation("loc-1")

    assertSame(getOrBuild(), getOrBuild())
    assertEquals(1, buildCount)
  }

  @Test
  fun `an entry expires one minute after it was built`() {
    inLocation("loc-1")
    val first = getOrBuild()

    advance(Duration.ofMinutes(1).minusNanos(1))
    assertSame(first, getOrBuild())

    advance(Duration.ofNanos(2))
    assertNotSame(first, getOrBuild())
    assertEquals(2, buildCount)
  }

  @Test
  fun `reads do not extend the life of an entry`() {
    inLocation("loc-1")
    val first = getOrBuild()

    repeat(5) {
      advance(Duration.ofSeconds(10))
      getOrBuild()
    }
    advance(Duration.ofSeconds(10))

    assertNotSame(first, getOrBuild())
  }

  @Test
  fun `two locations never share a snapshot`() {
    inLocation("loc-1")
    val locationOneSnapshot = getOrBuild()
    inLocation("loc-2")
    val locationTwoSnapshot = getOrBuild()

    assertNotSame(locationOneSnapshot, locationTwoSnapshot)
    assertEquals(2, buildCount)
    inLocation("loc-1")
    assertSame(locationOneSnapshot, getOrBuild())
  }

  @Test
  fun `two different windows in one location never share an entry`() {
    inLocation("loc-1")
    val first = getOrBuild()

    val laterWindow = cache.getOrBuild(ProductSalesReportWindow(window.from.plusDays(1), window.before.plusDays(1))) { snapshot() }
    val sameEndDifferentStart = cache.getOrBuild(ProductSalesReportWindow(window.from.minusDays(1), window.before)) { snapshot() }

    assertNotSame(first, laterWindow)
    assertNotSame(first, sameEndDifferentStart)
    assertNotSame(laterWindow, sameEndDifferentStart)
    assertSame(first, getOrBuild())
  }
}
