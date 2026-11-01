package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

// Not a @Cacheable: the global cache spec has no expiry.
// Not evicted on sale confirm or void: staleness is bounded by the TTL, which also covers today's partial day.
@Component
class ProductSalesDayBucketCache(ticker: Ticker = Ticker.systemTicker()) {

  private data class BucketKey(val locationSchema: String, val day: LocalDate)

  // Weighed by product rows, so a busy day costs more of the budget than a quiet one.
  private val bucketsByKey: Cache<BucketKey, ProductSalesDayBucket> = Caffeine.newBuilder()
    .expireAfterWrite(TIME_TO_LIVE)
    .maximumWeight(MAXIMUM_WEIGHT)
    .ticker(ticker)
    .weigher { _: BucketKey, bucket: ProductSalesDayBucket -> maxOf(1, bucket.rawRowsByLocationProductId.size) }
    .build()

  // loadMissingDays is called once with every uncached day, in ascending order, and must return a bucket for each.
  fun getOrLoad(
    days: List<LocalDate>,
    loadMissingDays: (missingDays: List<LocalDate>) -> Map<LocalDate, ProductSalesDayBucket>
  ): List<ProductSalesDayBucket> {
    val locationSchema = SessionContextProvider.getLocationSchema()
    val requestedKeys = days.map { day -> BucketKey(locationSchema, day) }
    val bucketsByRequestedKey = bucketsByKey.getAll(requestedKeys) { missingKeys ->
      loadMissingDays(missingKeys.map { missingKey -> missingKey.day }.sorted())
        .mapKeys { (day, _) -> BucketKey(locationSchema, day) }
    }
    return requestedKeys.map { requestedKey -> bucketsByRequestedKey.getValue(requestedKey) }
  }

  private companion object {
    val TIME_TO_LIVE: Duration = Duration.ofMinutes(15)
    const val MAXIMUM_WEIGHT = 200_000L
  }
}
