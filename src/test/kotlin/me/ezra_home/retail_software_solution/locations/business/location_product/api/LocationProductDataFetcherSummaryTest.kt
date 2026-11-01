package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEnricher
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductForSaleAssembler
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductPagedSearch
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductSummaryView
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class LocationProductDataFetcherSummaryTest {

  private data class FakeSummaryView(
    override val id: UUID,
    override val referenceNumber: String = "LP000001",
    override val orgProductId: UUID = UUID.randomUUID(),
    override val productName: String = "Rice",
    override val productGroupName: String = "Grains",
    override val baseUnitId: UUID = UUID.randomUUID()
  ) : LocationProductSummaryView

  private val locationProductRepository = mock(LocationProductRepository::class.java)
  private val locationProductDataFetcher = LocationProductDataFetcher(
    locationProductRepository,
    mock(UnitConversionGraphFacade::class.java),
    mock(LocationProductPagedSearch::class.java),
    mock(LocationProductEnricher::class.java),
    mock(LocationProductForSaleAssembler::class.java)
  )

  @Test
  fun `summaries are looked up in bounded chunks and merged`() {
    val chunkSizes = mutableListOf<Int>()
    `when`(locationProductRepository.findSummariesByIdIn(anyCollection())).thenAnswer {
      val chunk = it.getArgument<Collection<UUID>>(0)
      chunkSizes.add(chunk.size)
      chunk.map { chunkedId -> FakeSummaryView(chunkedId) }
    }
    val ids = List(25_000) { UUID.randomUUID() }

    val summariesById = locationProductDataFetcher.findSummaryByIds(ids)

    assertEquals(listOf(10_000, 10_000, 5_000), chunkSizes)
    assertEquals(ids.toSet(), summariesById.keys)
  }
}
