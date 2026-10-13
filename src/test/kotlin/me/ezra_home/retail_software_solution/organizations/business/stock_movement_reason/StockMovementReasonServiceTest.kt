package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonService
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class StockMovementReasonServiceTest {

    private val cache = mock(StockMovementReasonCache::class.java)
    private val service = StockMovementReasonService(
        cache,
        mock(StockMovementReasonMapper::class.java),
        mock(StockMovementReasonValidator::class.java)
    )

    private val theftReason = reasonDto("Theft", setOf(MovementType.STOCK_ADJUSTMENT_DECREASE), systemDefined = true)
    private val customReason = reasonDto("Samples", setOf(MovementType.SALE), systemDefined = false)

    init {
        `when`(cache.getAll()).thenReturn(listOf(theftReason, customReason))
    }

    @Test
    fun `a reason is mandatory on a required movement type`() {
        assertThrows(RtsGenericException::class.java) { service.guardReason(MovementType.STOCK_ADJUSTMENT_DECREASE, null) }
        assertThrows(RtsGenericException::class.java) { service.guardReason(MovementType.STOCK_ADJUSTMENT_INCREASE, null) }
    }

    @Test
    fun `a missing reason is accepted on every optional movement type`() {
        MovementType.entries.filterNot { it.reasonRequired }.forEach { service.guardReason(it, null) }
    }

    @Test
    fun `a listed movement type accepts the reason`() {
        service.guardReason(MovementType.STOCK_ADJUSTMENT_DECREASE, theftReason.id)
        service.guardReason(MovementType.SALE, customReason.id)
    }

    @Test
    fun `a reason is rejected on a movement type it does not list`() {
        assertThrows(RtsGenericException::class.java) { service.guardReason(MovementType.CUSTOMER_RETURN, theftReason.id) }
    }

    @Test
    fun `any reason is rejected on a movement type no reason lists`() {
        assertThrows(RtsGenericException::class.java) { service.guardReason(MovementType.PURCHASE_RECEIVED, theftReason.id) }
        assertThrows(RtsGenericException::class.java) { service.guardReason(MovementType.OPENING_STOCK, customReason.id) }
    }

    @Test
    fun `an unknown reason id is rejected`() {
        assertThrows(RtsGenericException::class.java) { service.guardReason(MovementType.STOCK_ADJUSTMENT_DECREASE, UUID.randomUUID()) }
    }

    @Test
    fun `updating a system reason is rejected including its movement types`() {
        val updateDto = StockMovementReasonUpdateDto(
            id = theftReason.id,
            name = Optional.of("Renamed"),
            movementTypes = setOf(MovementType.STOCK_ADJUSTMENT_INCREASE)
        )

        assertThrows(RtsGenericException::class.java) { service.update(updateDto) }
        assertTrue(mockingDetails(cache).invocations.none { it.method.name == "save" })
    }

    @Test
    fun `deleting a system reason is rejected`() {
        assertThrows(RtsGenericException::class.java) { service.delete(theftReason.id) }
        assertTrue(mockingDetails(cache).invocations.none { it.method.name == "delete" })
    }

    @Test
    fun `deleting an org reason removes it`() {
        service.delete(customReason.id)

        verify(cache).delete(customReason.id)
    }

    @Test
    fun `deleting an unknown reason does nothing`() {
        service.delete(UUID.randomUUID())

        assertTrue(mockingDetails(cache).invocations.none { it.method.name == "delete" })
    }

    private fun reasonDto(name: String, movementTypes: Set<MovementType>, systemDefined: Boolean) = StockMovementReasonDto(
        id = UUID.randomUUID(),
        referenceNumber = "SMR000001",
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
        code = if (systemDefined) name.take(3).uppercase() else null,
        name = name,
        description = null,
        movementTypes = movementTypes,
        systemDefined = systemDefined
    )
}
