package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRevisionValidator
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryContext
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class PurchaseDeliveryLineReplacerTest {

    private val purchaseDeliveryLineRepository = mock(PurchaseDeliveryLineRepository::class.java)
    private val purchaseDeliveryRepository = mock(PurchaseDeliveryRepository::class.java)
    private val purchaseDeliveryRevisionValidator = mock(PurchaseDeliveryRevisionValidator::class.java)
    private val deliveryHandlerForPurchase = mock(DeliveryHandlerForPurchase::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val purchaseDeliveryLineReplacer = PurchaseDeliveryLineReplacer(
        purchaseDeliveryLineRepository,
        purchaseDeliveryRepository,
        purchaseDeliveryRevisionValidator,
        deliveryHandlerForPurchase,
        locationProductDataFetcher
    )

    private val deliveryId = UUID.randomUUID()
    private val revisedLine = PurchaseDeliveryLineEntity(
        purchaseDeliveryId = deliveryId,
        purchaseLineId = UUID.randomUUID(),
        userProvidedQuantityDelivered = BigDecimal("10"),
        unitCost = BigDecimal("5.0000"),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 12L,
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = "DELN000001"
    }

    private val purchaseLineDto = PurchaseLineDto(
        id = revisedLine.purchaseLineId,
        purchaseId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        unitCost = BigDecimal("5.0000"),
        storageUnitsPerUserProvidedUnit = 12L,
        unitId = revisedLine.unitId,
        storageQuantityExpected = ExactQuantity.ofStorage(BigDecimal("240")),
        storageQuantityRemaining = ExactQuantity.ZERO,
    )

    private fun givenStorageUnitsPerUserProvidedUnit(unitId: UUID, storageUnitsPerUserProvidedUnit: Long) {
        val locationProductUnitRequest = LocationProductUnitRequestDto(purchaseLineDto.locationProductId, unitId)
        `when`(locationProductDataFetcher.getStorageUnitsByRequest(listOf(locationProductUnitRequest)))
            .thenReturn(mapOf(locationProductUnitRequest to storageUnitsPerUserProvidedUnit))
    }

    private fun stubRepositories() {
        givenStorageUnitsPerUserProvidedUnit(revisedLine.unitId, 12L)
        `when`(purchaseDeliveryLineRepository.findById(revisedLine.id!!)).thenReturn(Optional.of(revisedLine))
        val purchaseId = UUID.randomUUID()
        `when`(purchaseDeliveryRepository.getReferenceById(deliveryId)).thenReturn(
            PurchaseDeliveryEntity(
                purchaseId,
                deliveredAt = OffsetDateTime.now()
            ).also { it.referenceNumber = "DEL000001" }
        )
        `when`(deliveryHandlerForPurchase.getDeliveryContext(purchaseId)).thenReturn(
            PurchaseDeliveryContext(
                purchaseId,
                "PUR000001",
                UUID.randomUUID(),
                mapOf(revisedLine.purchaseLineId to purchaseLineDto)
            )
        )
        `when`(
            purchaseDeliveryLineRepository.save(any(PurchaseDeliveryLineEntity::class.java))
        ).thenAnswer { invocation ->
            invocation.getArgument<PurchaseDeliveryLineEntity>(0).also {
                if (it !== revisedLine) it.referenceNumber = "DELN000002"
            }
        }
    }

    @Test
    fun `the replacement is a sibling that keeps the purchase line and unit, and the revised line becomes REVISED`() {
        stubRepositories()

        val replacement = purchaseDeliveryLineReplacer.replace(validatedRevision("0", "6.50"))

        assertEquals(PurchaseDeliveryLineStatus.REVISED, revisedLine.status)
        assertEquals("DEL000001", replacement.deliveryReferenceNumber)
        assertEquals("DELN000001", replacement.revisedLineReferenceNumber)
        assertEquals("DELN000002", replacement.replacementLineReferenceNumber)
        assertEquals(revisedLine.purchaseLineId, replacement.purchaseLineId)
        assertEquals(revisedLine.unitId, replacement.unitId)
        assertEquals(12L, replacement.storageUnitsPerUserProvidedUnit)
        assertEquals(BigDecimal("0"), replacement.userProvidedQuantityDelivered)
        assertEquals(BigDecimal("6.50"), replacement.unitCost)
    }

    @Test
    fun `validation reports the problems and the product without throwing or writing`() {
        stubRepositories()
        val problem = BlockingProblem("DELN000001", "Already changed.", "Change its latest line.")
        `when`(
            purchaseDeliveryRevisionValidator.problemsOf(
                "DEL000001", purchaseLineDto, revisedLine, ExactQuantity.ofStorage(BigDecimal("48"))
            )
        ).thenReturn(listOf(problem))

        val validated = purchaseDeliveryLineReplacer.validateRevisionReportingProblems(
            PurchaseDeliveryLineReplaceRequest(
                revisedLine.id!!,
                UserProvidedQuantity.of(BigDecimal("4")),
                revisedLine.unitId,
                Money.of(BigDecimal("5.0000"))
            )
        )

        assertEquals(listOf(problem), validated.problems)
        assertEquals(purchaseLineDto.locationProductId, validated.locationProductId)
        verify(purchaseDeliveryLineRepository, never()).save(any(PurchaseDeliveryLineEntity::class.java))
    }

    @Test
    fun `a revision in another unit is converted with that unit and compared with the line's own unit`() {
        stubRepositories()
        val pieceUnitId = UUID.randomUUID()
        givenStorageUnitsPerUserProvidedUnit(pieceUnitId, 1L)
        `when`(purchaseDeliveryRevisionValidator.problemsOf(
            "DEL000001", purchaseLineDto, revisedLine, ExactQuantity.ofStorage(BigDecimal("30"))
        )).thenReturn(emptyList())

        val validated = purchaseDeliveryLineReplacer.validateRevisionReportingProblems(
            PurchaseDeliveryLineReplaceRequest(
                revisedLine.id!!, UserProvidedQuantity.of(BigDecimal("30")), pieceUnitId,
                Money.of(BigDecimal("0.4000"))
            )
        )

        assertEquals(revisedLine.unitId, validated.revisedUnitId)
        assertEquals(pieceUnitId, validated.unitId)
        assertEquals(1L, validated.storageUnitsPerUserProvidedUnit)
        assertEquals(ExactQuantity.ofStorage(BigDecimal("30")), validated.replacementStorageQuantity)
        assertEquals(ExactQuantity.ofStorage(BigDecimal("-90")), validated.storageQuantityChange)
    }

    @Test
    fun `an unknown delivery line is reported as not found`() {
        `when`(purchaseDeliveryLineRepository.findById(revisedLine.id!!)).thenReturn(Optional.empty())

        val exception = assertThrows(RtsGenericException::class.java) {
            purchaseDeliveryLineReplacer.validateRevisionReportingProblems(
                PurchaseDeliveryLineReplaceRequest(
                    revisedLine.id!!,
                    UserProvidedQuantity.of(BigDecimal("4")),
                    revisedLine.unitId,
                    Money.of(BigDecimal("5.0000"))
                )
            )
        }

        assertEquals("That delivery line could not be found. Refresh the page and try again.", exception.message)
    }

    @Test
    fun `a revision with problems is rejected before any row is written`() {
        stubRepositories()
        val problem = BlockingProblem("DLL000001", "already changed", "change the latest line")

        val exception = assertThrows(BlockedChangeException::class.java) {
            purchaseDeliveryLineReplacer.replace(validatedRevision("4", "5.0000", listOf(problem)))
        }

        assertEquals(listOf(problem), exception.problems)
        verify(purchaseDeliveryLineRepository, never()).save(any(PurchaseDeliveryLineEntity::class.java))
        assertEquals(PurchaseDeliveryLineStatus.ACTIVE, revisedLine.status)
    }

    private fun validatedRevision(
        userProvidedQuantity: String,
        unitCost: String,
        problems: List<BlockingProblem> = emptyList()
    ) = ValidatedDeliveryLineRevisionDto(
        revisedDeliveryLineId = revisedLine.id!!,
        deliveryReferenceNumber = "DEL000001",
        revisedLineReferenceNumber = "DELN000001",
        purchaseLineId = revisedLine.purchaseLineId,
        locationProductId = purchaseLineDto.locationProductId,
        revisedUnitId = revisedLine.unitId,
        revisedStorageUnitsPerUserProvidedUnit = 12L,
        unitId = revisedLine.unitId,
        storageUnitsPerUserProvidedUnit = 12L,
        revisedUserProvidedQuantityDelivered = BigDecimal("10"),
        revisedUnitCost = BigDecimal("5.0000"),
        userProvidedQuantityDelivered = BigDecimal(userProvidedQuantity),
        unitCost = BigDecimal(unitCost),
        problems = problems,
    )
}
