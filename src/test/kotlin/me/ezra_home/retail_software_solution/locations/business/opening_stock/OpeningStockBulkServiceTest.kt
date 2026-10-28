package me.ezra_home.retail_software_solution.locations.business.opening_stock

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUpdateDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockBulkLineDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockBulkService
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockLineDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockResponseDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class OpeningStockBulkServiceTest {

    private val openingStockService = mock(OpeningStockService::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val locationProductService = mock(LocationProductService::class.java)
    private val unitValueFetcher = mock(UnitValueFetcher::class.java)
    private val openingStockBulkService = OpeningStockBulkService(
        openingStockService, locationProductDataFetcher, locationProductService, unitValueFetcher
    )

    private val colaLocationProductId = UUID.randomUUID()
    private val chipsLocationProductId = UUID.randomUUID()
    private val pieceUnitId = UUID.randomUUID()
    private val boxUnitId = UUID.randomUUID()

    @BeforeEach
    fun stubReferenceData() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(
            listOf(unitValueResponseDto("PC", pieceUnitId), unitValueResponseDto("BOX", boxUnitId))
        )
    }

    @Test
    fun `resolves references and unit codes, declares the stock and then writes each default sale price`() {
        `when`(locationProductDataFetcher.findIdsByReferenceNumbers(listOf("LP000001", "LP000002")))
            .thenReturn(mapOf("LP000001" to colaLocationProductId, "LP000002" to chipsLocationProductId))
        val colaRow = bulkLine("LP000001", "PC", "12.5", "1.50", "2.00")
        val chipsRow = bulkLine("  LP000002 ", " BOX ", "3", "10.00", "15.00")
        val expectedOpeningStockLines = listOf(
            OpeningStockLineDto(colaLocationProductId, colaRow.userProvidedQuantity, pieceUnitId, colaRow.unitCost),
            OpeningStockLineDto(chipsLocationProductId, chipsRow.userProvidedQuantity, boxUnitId, chipsRow.unitCost)
        )
        val declared = listOf(openingStockResponseDto(colaLocationProductId), openingStockResponseDto(chipsLocationProductId))
        `when`(openingStockService.declareInitialStock(expectedOpeningStockLines)).thenReturn(declared)

        val result = openingStockBulkService.bulkDeclareInitialStock(listOf(colaRow, chipsRow))

        assertSame(declared, result)
        verify(locationProductService).updateProduct(
            LocationProductUpdateDto(colaLocationProductId, Optional.of(colaRow.defaultSalePrice))
        )
        verify(locationProductService).updateProduct(
            LocationProductUpdateDto(chipsLocationProductId, Optional.of(chipsRow.defaultSalePrice))
        )
    }

    @Test
    fun `reports every unresolvable row together and declares nothing`() {
        `when`(locationProductDataFetcher.findIdsByReferenceNumbers(listOf("LP000001", "LP999999", "LP000002")))
            .thenReturn(mapOf("LP000001" to colaLocationProductId, "LP000002" to chipsLocationProductId))

        val exception = assertThrows(RtsGenericException::class.java) {
            openingStockBulkService.bulkDeclareInitialStock(
                listOf(
                    bulkLine("LP000001", "PC", "1", "1.00", "2.00"),
                    bulkLine("LP999999", "PC", "1", "1.00", "2.00"),
                    bulkLine("LP000002", "NOPE", "1", "1.00", "2.00")
                )
            )
        }

        assertEquals(
            listOf("Row 2: Unknown product 'LP999999'", "Row 3: Unknown unit code 'NOPE'"),
            exception.payload
        )
        verifyNoInteractions(openingStockService, locationProductService)
    }

    @Test
    fun `reports blank reference numbers and unit codes alongside other row errors`() {
        `when`(locationProductDataFetcher.findIdsByReferenceNumbers(listOf("LP999999")))
            .thenReturn(emptyMap())

        val exception = assertThrows(RtsGenericException::class.java) {
            openingStockBulkService.bulkDeclareInitialStock(
                listOf(
                    bulkLine("  ", "PC", "1", "1.00", "2.00"),
                    bulkLine("LP999999", "", "1", "1.00", "2.00"),
                    bulkLine("LP999999", "PC", "1", "1.00", "2.00")
                )
            )
        }

        assertEquals(
            listOf(
                "Row 1: Missing product reference number",
                "Row 2: Missing unit code",
                "Row 3: Unknown product 'LP999999'"
            ),
            exception.payload
        )
        verifyNoInteractions(openingStockService, locationProductService)
    }

    @Test
    fun `rejects a product that appears twice in the request`() {
        `when`(locationProductDataFetcher.findIdsByReferenceNumbers(listOf("LP000001", "LP000001")))
            .thenReturn(mapOf("LP000001" to colaLocationProductId))

        val exception = assertThrows(RtsGenericException::class.java) {
            openingStockBulkService.bulkDeclareInitialStock(
                listOf(
                    bulkLine("LP000001", "PC", "1", "1.00", "2.00"),
                    bulkLine("LP000001", "BOX", "2", "1.00", "2.00")
                )
            )
        }

        assertEquals(listOf("Row 2: Product 'LP000001' appears more than once in the request"), exception.payload)
        verifyNoInteractions(openingStockService, locationProductService)
    }

    @Test
    fun `leaves prices untouched when the declaration itself fails`() {
        `when`(locationProductDataFetcher.findIdsByReferenceNumbers(listOf("LP000001")))
            .thenReturn(mapOf("LP000001" to colaLocationProductId))
        `when`(openingStockService.declareInitialStock(listOf(openingStockLine(colaLocationProductId))))
            .thenThrow(RtsGenericException("Opening stock already declared"))

        assertThrows(RtsGenericException::class.java) {
            openingStockBulkService.bulkDeclareInitialStock(listOf(bulkLine("LP000001", "PC", "1", "1.00", "2.00")))
        }

        verifyNoInteractions(locationProductService)
    }

    private fun openingStockLine(locationProductId: UUID): OpeningStockLineDto {
        val row = bulkLine("LP000001", "PC", "1", "1.00", "2.00")
        return OpeningStockLineDto(locationProductId, row.userProvidedQuantity, pieceUnitId, row.unitCost)
    }

    private fun bulkLine(referenceNumber: String, unitCode: String, quantity: String, unitCost: String, salePrice: String) =
        OpeningStockBulkLineDto(
            locationProductReferenceNumber = referenceNumber,
            userProvidedQuantity = PositiveUserProvidedQuantity.of(BigDecimal(quantity)),
            unitCode = unitCode,
            unitCost = PositiveMoney.of(BigDecimal(unitCost)),
            defaultSalePrice = Money.of(BigDecimal(salePrice))
        )

    private fun unitValueResponseDto(code: String, id: UUID) = UnitValueResponseDto(
        id = id,
        name = code,
        code = code,
        description = null,
        baseUnit = null,
        baseUnitName = null,
        unitsOfBasePerUnit = null,
        createdBy = "tester",
        createdOn = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        unitGroupId = UUID.randomUUID(),
        referenceNumber = "UV$code",
        systemDefined = false
    )

    private fun openingStockResponseDto(locationProductId: UUID) = OpeningStockResponseDto(
        referenceNumber = "OS000001",
        locationProductId = locationProductId,
        productLabel = "label",
        userProvidedQuantity = BigDecimal.ONE,
        unitId = pieceUnitId,
        unitCost = BigDecimal.ONE,
        declaredBy = "tester",
        declaredAt = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    )
}
