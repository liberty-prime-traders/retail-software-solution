package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.locations.business.stock.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class StockMovementReasonSeederTest {

    private val cache = mock(StockMovementReasonCache::class.java)
    private val seeder = StockMovementReasonSeeder(cache)

    @Test
    fun `seeding a new organization inserts every system reason with its movement types`() {
        `when`(cache.getAll()).thenReturn(emptyList())

        seeder.seed()

        val insertedEntities = insertedEntities()
        assertEquals(StockMovementReason.entries.map { it.code }.toSet(), insertedEntities.map { it.code }.toSet())
        insertedEntities.forEach { entity ->
            val systemReason = StockMovementReason.entries.first { it.code == entity.code }
            assertEquals(systemReason.allowedMovementTypes, entity.movementTypes, systemReason.name)
            assertTrue(entity.systemDefined)
        }
    }

    @Test
    fun `re-seeding an up to date organization writes nothing`() {
        `when`(cache.getAll()).thenReturn(StockMovementReason.entries.map { systemReasonDto(it, it.allowedMovementTypes) })

        seeder.seed()

        assertTrue(mockingDetails(cache).invocations.none { it.method.name in setOf("saveAll", "save") })
    }

    @Test
    fun `re-seeding syncs a changed movement type set onto the existing system row`() {
        val staleTheftReason = systemReasonDto(StockMovementReason.THEFT, setOf(MovementType.SALE))
        val upToDateReasons = StockMovementReason.entries
            .filter { it != StockMovementReason.THEFT }
            .map { systemReasonDto(it, it.allowedMovementTypes) }
        `when`(cache.getAll()).thenReturn(upToDateReasons + staleTheftReason)

        seeder.seed()

        verify(cache).getAll()
        verify(cache).save(staleTheftReason.copy(movementTypes = StockMovementReason.THEFT.allowedMovementTypes))
        verifyNoMoreInteractions(cache)
    }

    @Test
    fun `an org-created reason never matches a system code and is left alone`() {
        val orgCreatedReason = systemReasonDto(StockMovementReason.THEFT, setOf(MovementType.SALE))
            .copy(code = null, systemDefined = false)
        `when`(cache.getAll()).thenReturn(listOf(orgCreatedReason))

        seeder.seed()

        assertEquals(StockMovementReason.entries.size, insertedEntities().size)
        assertTrue(mockingDetails(cache).invocations.none { it.method.name == "save" })
    }

    private fun insertedEntities(): List<StockMovementReasonEntity> =
        mockingDetails(cache).invocations
            .filter { it.method.name == "saveAll" }
            .flatMap { invocation -> invocation.arguments.first() as Collection<*> }
            .map { it as StockMovementReasonEntity }

    private fun systemReasonDto(reason: StockMovementReason, movementTypes: Set<MovementType>) = StockMovementReasonDto(
        id = UUID.randomUUID(),
        referenceNumber = "SMR000001",
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
        code = reason.code,
        name = reason.displayName,
        description = reason.description,
        movementTypes = movementTypes,
        systemDefined = true
    )
}
