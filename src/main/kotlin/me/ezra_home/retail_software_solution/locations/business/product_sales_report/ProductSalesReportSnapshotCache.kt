package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

// Not a @Cacheable: the global cache spec has no expiry.
// Holds the combined, ranked rows so that paging and re-sorting a range never re-assemble product identities. The
// TTL is short because the day buckets underneath already bound how stale the figures can be.
@Component
class ProductSalesReportSnapshotCache(ticker: Ticker = Ticker.systemTicker()) {

  private val snapshotsByKey: Cache<SnapshotKey, ProductSalesReportSnapshot> =
    Caffeine.newBuilder().expireAfterWrite(TIME_TO_LIVE).maximumSize(MAXIMUM_SIZE).ticker(ticker).build()

  private data class SnapshotKey(val locationSchema: String, val windowFrom: LocalDate, val windowBefore: LocalDate)

  fun getOrBuild(window: ProductSalesReportWindow, snapshotBuilder: () -> ProductSalesReportSnapshot): ProductSalesReportSnapshot =
    snapshotsByKey.get(SnapshotKey(SessionContextProvider.getLocationSchema(), window.from, window.before)) { snapshotBuilder() }

  private companion object {
    val TIME_TO_LIVE: Duration = Duration.ofMinutes(1)
    const val MAXIMUM_SIZE = 20L
  }
}
