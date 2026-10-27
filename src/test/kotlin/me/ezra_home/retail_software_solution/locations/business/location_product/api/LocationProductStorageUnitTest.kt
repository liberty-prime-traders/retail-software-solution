package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEntity
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEnricher
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductForSaleAssembler
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductPagedSearch
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitPlacement
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class LocationProductStorageUnitTest {

    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val gramId = UUID.randomUUID()
    private val miscPieceId = UUID.randomUUID()
    private val miscCartonId = UUID.randomUUID()
    private val locationProductRepository = mock(LocationProductRepository::class.java)
    private val unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java).also {
        `when`(it.getOrLoad()).thenReturn(
            UnitConversionGraph(
                emptyMap(),
                emptyMap(),
                mapOf(
                    pieceId to UnitPlacement(pieceId, 1L),
                    baleId to UnitPlacement(pieceId, 18L),
                    gramId to UnitPlacement(gramId, 1L),
                    miscPieceId to UnitPlacement(miscPieceId, 1L),
                    miscCartonId to UnitPlacement(miscCartonId, 1L),
                )
            )
        )
    }
    private val fetcher = LocationProductDataFetcher(
        locationProductRepository,
        unitConversionGraphFacade,
        mock(LocationProductPagedSearch::class.java),
        mock(LocationProductEnricher::class.java),
        mock(LocationProductForSaleAssembler::class.java),
    )

    private fun productWithBaseUnit(baseUnitId: UUID): UUID {
        val product = LocationProductEntity(
            orgProductId = UUID.randomUUID(), productName = "P", productGroupName = "G", categoryId = UUID.randomUUID(), baseUnitId = baseUnitId
        ).also { it.id = UUID.randomUUID() }
        `when`(locationProductRepository.findAllById(listOf(product.id!!))).thenReturn(listOf(product))
        return product.id!!
    }

    @Test
    fun `a unit of the product's group captures its size in the storage unit`() {
        val locationProductId = productWithBaseUnit(baleId)

        val captured = fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, pieceId)))

        assertEquals(1L, captured.getValue(locationProductId))
        assertEquals(
            18L,
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, baleId))).getValue(locationProductId)
        )
    }

    @Test
    fun `a unit whose root differs from the product's storage unit is rejected`() {
        val locationProductId = productWithBaseUnit(baleId)

        val exception = assertThrows(RtsGenericException::class.java) {
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, gramId)))
        }

        assertEquals(LocationProductDataFetcher.UNIT_OUTSIDE_PRODUCT_GROUP, exception.message)
    }

    @Test
    fun `a miscellaneous product is limited to its own base unit with no special case`() {
        val locationProductId = productWithBaseUnit(miscPieceId)

        assertEquals(
            1L,
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, miscPieceId))).getValue(locationProductId)
        )
        assertThrows(RtsGenericException::class.java) {
            fetcher.getStorageUnitsPerUserProvidedUnit(listOf(LocationProductUnitRequestDto(locationProductId, miscCartonId)))
        }
    }
}
