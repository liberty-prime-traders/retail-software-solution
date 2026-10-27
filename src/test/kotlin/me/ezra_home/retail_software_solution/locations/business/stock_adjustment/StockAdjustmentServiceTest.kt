package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentDecreaseStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentIncreaseStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCreateDto
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentDirection
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonService
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockAdjustmentServiceTest {

    private val locationProductId = UUID.randomUUID()
    private val boxUnitId = UUID.randomUUID()
    private val stockMovementReasonId = UUID.randomUUID()
    private val creatorId = UUID.randomUUID()
    private val stockAdjustmentRepository = mock(StockAdjustmentRepository::class.java) { invocation ->
        if (invocation.method.name == "save") {
            (invocation.arguments[0] as StockAdjustmentEntity).also {
                it.createdById = creatorId
                it.createdOn = OffsetDateTime.parse("2026-01-01T00:00:00Z")
            }
        } else {
            null
        }
    }
    private val increaseRequests = mutableListOf<StockAdjustmentIncreaseStockRequest>()
    private val decreaseRequests = mutableListOf<StockAdjustmentDecreaseStockRequest>()
    private val stockAdjustmentStockUpdater = mock(StockAdjustmentStockUpdater::class.java) { invocation ->
        when (invocation.method.name.substringBefore("-")) {
            "increaseStock" -> {
                increaseRequests.add(invocation.arguments[0] as StockAdjustmentIncreaseStockRequest)
                null
            }
            "decreaseStock" -> {
                decreaseRequests.add(invocation.arguments[0] as StockAdjustmentDecreaseStockRequest)
                BigDecimal("12.0000")
            }
            else -> null
        }
    }
    private val publishedStockAdjustments = mutableListOf<StockAdjustmentEntity>()
    private val stockAdjustmentHandlerForKafka = mock(StockAdjustmentHandlerForKafka::class.java) { invocation ->
        publishedStockAdjustments.add(invocation.arguments[0] as StockAdjustmentEntity)
        null
    }
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val guardedReasons = mutableListOf<Pair<MovementType, UUID?>>()
    private val stockMovementReasonService = mock(StockMovementReasonService::class.java) { invocation ->
        if (invocation.method.name == "guardReason") {
            guardedReasons.add(invocation.arguments[0] as MovementType to invocation.arguments[1] as UUID?)
        }
        null
    }
    private val referenceNumberGenerator = mock(ReferenceNumberGenerator::class.java)
    private val userQualifier = mock(UserQualifier::class.java)
    private val stockAdjustmentService = StockAdjustmentService(
        stockAdjustmentRepository,
        stockAdjustmentStockUpdater,
        stockAdjustmentHandlerForKafka,
        mock(LocationProductService::class.java),
        locationProductDataFetcher,
        stockMovementReasonService,
        mock(FiscalPeriodService::class.java),
        mock(EntityAdvisoryLock::class.java),
        referenceNumberGenerator,
        userQualifier
    )

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
        `when`(referenceNumberGenerator.generateSingle(TableName.STOCK_ADJUSTMENT)).thenReturn("STADJ000001")
        `when`(userQualifier.getCreatorFullName(creatorId)).thenReturn("Test User")
        `when`(locationProductDataFetcher.getStorageUnitsPerUserProvidedUnit(anyUnitRequests())).thenReturn(mapOf(locationProductId to 12L))
        `when`(locationProductDataFetcher.findSummaryByIds(setOf(locationProductId))).thenReturn(
            mapOf(locationProductId to LocationProductSummaryDto(locationProductId, "LP000001", "Rice", "Grains", UUID.randomUUID()))
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `free goods increase prices the layer from the entered unit cost and carries category and note onto the movement`() {
        val response = stockAdjustmentService.adjust(
            createDto(StockAdjustmentCategory.FREE_GOODS, "3", unitCost = "10.50", note = "Bonus cartons on PO-12")
        )

        val increaseRequest = increaseRequests.single()
        assertEquals(BigDecimal("31.50"), increaseRequest.totalCost)
        assertEquals(0, BigDecimal("36").compareTo(increaseRequest.storageQuantity.toStorageQuantity()))
        assertEquals("STADJ000001", increaseRequest.documentReference)
        assertEquals(StockAdjustmentCategory.FREE_GOODS, increaseRequest.adjustmentDetails.category)
        assertEquals("Bonus cartons on PO-12", increaseRequest.adjustmentDetails.note)
        assertEquals(null, increaseRequest.adjustmentDetails.reasonId)
        assertEquals(StockAdjustmentDirection.INCREASE, response.direction)
        assertEquals(BigDecimal("31.50"), response.totalCost)
        assertEquals(BigDecimal("3"), response.userProvidedQuantity)
        assertEquals("STADJ000001", publishedStockAdjustments.single().referenceNumber)
        assertEquals(emptyList<Pair<MovementType, UUID?>>(), guardedReasons)
    }

    @Test
    fun `opening stock increase requires the entered unit cost`() {
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(createDto(StockAdjustmentCategory.OPENING_STOCK, "3", note = "Count at go-live"))
        }
        assertEquals(emptyList<StockAdjustmentIncreaseStockRequest>(), increaseRequests)
    }

    @Test
    fun `internal use decrease persists the cost consumed from the layers`() {
        val response = stockAdjustmentService.adjust(createDto(StockAdjustmentCategory.INTERNAL_USE, "2", note = "Shop supplies"))

        assertEquals(0, BigDecimal("24").compareTo(decreaseRequests.single().storageQuantity.toStorageQuantity()))
        assertEquals(BigDecimal("12.0000"), response.totalCost)
        assertEquals(StockAdjustmentDirection.DECREASE, response.direction)
    }

    @Test
    fun `internal use rejects a unit cost because the cost comes from stock`() {
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(createDto(StockAdjustmentCategory.INTERNAL_USE, "3", unitCost = "10", note = "Supplies"))
        }
    }

    @Test
    fun `categories other than other require a note and reject a reason`() {
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(createDto(StockAdjustmentCategory.INTERNAL_USE, "1"))
        }
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(
                createDto(StockAdjustmentCategory.INTERNAL_USE, "1", note = "Supplies", stockMovementReasonId = stockMovementReasonId)
            )
        }
    }

    @Test
    fun `a fixed-direction category rejects the opposite direction`() {
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(
                createDto(StockAdjustmentCategory.INTERNAL_USE, "1", note = "Supplies", direction = StockAdjustmentDirection.INCREASE)
            )
        }
    }

    @Test
    fun `other requires a direction and a reason that is guarded against the direction`() {
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(createDto(StockAdjustmentCategory.OTHER, "1", stockMovementReasonId = stockMovementReasonId))
        }
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(createDto(StockAdjustmentCategory.OTHER, "1", direction = StockAdjustmentDirection.DECREASE))
        }

        stockAdjustmentService.adjust(
            createDto(
                StockAdjustmentCategory.OTHER, "1",
                direction = StockAdjustmentDirection.DECREASE, stockMovementReasonId = stockMovementReasonId
            )
        )

        assertEquals(listOf(StockAdjustmentDirection.DECREASE.movementType to stockMovementReasonId), guardedReasons)
        assertEquals(stockMovementReasonId, decreaseRequests.single().adjustmentDetails.reasonId)
    }

    @Test
    fun `other increase is priced at the supplied unit cost per typed unit`() {
        val response = stockAdjustmentService.adjust(
            createDto(
                StockAdjustmentCategory.OTHER, "3", unitCost = "6",
                direction = StockAdjustmentDirection.INCREASE, stockMovementReasonId = stockMovementReasonId
            )
        )

        assertEquals(BigDecimal("18.00"), increaseRequests.single().totalCost)
        assertEquals(BigDecimal("18.00"), response.totalCost)
    }

    @Test
    fun `other increase without a unit cost is rejected before stock changes`() {
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(
                createDto(
                    StockAdjustmentCategory.OTHER, "3",
                    direction = StockAdjustmentDirection.INCREASE, stockMovementReasonId = stockMovementReasonId
                )
            )
        }
        assertEquals(emptyList<StockAdjustmentIncreaseStockRequest>(), increaseRequests)
    }

    @Test
    fun `other decrease rejects a unit cost because FIFO supplies it`() {
        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentService.adjust(
                createDto(
                    StockAdjustmentCategory.OTHER, "3", unitCost = "6",
                    direction = StockAdjustmentDirection.DECREASE, stockMovementReasonId = stockMovementReasonId
                )
            )
        }
        assertEquals(emptyList<StockAdjustmentDecreaseStockRequest>(), decreaseRequests)
    }

    @Test
    fun `quantity beyond the allowed decimal places is rejected instead of rounded`() {
        assertThrows(RtsGenericException::class.java) {
            createDto(StockAdjustmentCategory.FREE_GOODS, "0.333", unitCost = "10", note = "Bonus")
        }
    }

    private fun createDto(
        category: StockAdjustmentCategory,
        quantity: String,
        direction: StockAdjustmentDirection? = null,
        unitCost: String? = null,
        note: String? = null,
        stockMovementReasonId: UUID? = null
    ) = StockAdjustmentCreateDto(
        locationProductId = locationProductId,
        category = category,
        direction = direction,
        userProvidedQuantity = PositiveUserProvidedQuantity.of(BigDecimal(quantity)),
        unitId = boxUnitId,
        stockMovementReasonId = stockMovementReasonId,
        note = note,
        unitCost = unitCost?.let { PositiveMoney.of(BigDecimal(it)) },
    )

    private fun anyUnitRequests(): List<LocationProductUnitRequestDto> = anyList()
}
