package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class UnitGroupSeederTest {

    private val unitGroupCache = mock(UnitGroupCache::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)
    private val unitGroupSeeder = UnitGroupSeeder(unitGroupCache, orgEntityAdvisoryLock)
    private val savedUnitGroups = mutableListOf<UnitGroupEntity>()

    @BeforeEach
    fun recordSavedUnitGroups() {
        `when`(unitGroupCache.saveAll(anyCollection())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[0] as Collection<UnitGroupEntity>).also { savedUnitGroups.addAll(it) }.toList()
        }
    }

    @Test
    fun `an empty organization gets every system group, after the name lock is taken`() {
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(emptyList())

        unitGroupSeeder.seed()

        val order = inOrder(orgEntityAdvisoryLock, unitGroupCache)
        order.verify(orgEntityAdvisoryLock).acquire(LockNamespaces.UNIT_GROUP_NAME, UnitGroupNameRules.LOCK_KEY)
        order.verify(unitGroupCache).getAllUnitGroups()
        assertEquals(SystemUnitGroup.entries.map { it.code }, savedUnitGroups.map { it.code })
    }

    @Test
    fun `seeding again inserts nothing once every system group exists`() {
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(SystemUnitGroup.entries.map { unitGroupDto(it.groupName, it.code) })

        unitGroupSeeder.seed()

        verify(unitGroupCache, never()).saveAll(anyCollection())
    }

    @Test
    fun `a custom group already holding a system group's name stops the seed instead of duplicating it`() {
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(listOf(unitGroupDto("Miscellaneous", code = null)))

        assertThrows(RtsGenericException::class.java) { unitGroupSeeder.seed() }

        verify(unitGroupCache, never()).saveAll(anyCollection())
    }

    private fun unitGroupDto(name: String, code: String?) = UnitGroupDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-1",
        code = code,
        name = name,
        description = null,
        systemDefined = code != null
    )
}
