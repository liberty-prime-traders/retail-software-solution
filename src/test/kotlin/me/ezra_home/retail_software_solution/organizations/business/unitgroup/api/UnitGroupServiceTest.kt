package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupMapper
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class UnitGroupServiceTest {

    @Test
    fun `createUnitGroup provisions a default Piece unit for a normal group`() {
        val unitGroupMapper = mock(UnitGroupMapper::class.java)
        val unitGroupCache = mock(UnitGroupCache::class.java)
        val unitValueService = mock(UnitValueService::class.java)

        val insertDto = UnitGroupInsertDto(name = "Boxes", description = null)
        val groupId = UUID.randomUUID()
        val groupDto = unitGroupDto(groupId, "Boxes")
        val responseDto = unitGroupResponseDto(groupId, "Boxes")

        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(emptyList())
        `when`(unitGroupCache.create(insertDto)).thenReturn(groupDto)
        `when`(unitGroupMapper.toResponseDto(groupDto)).thenReturn(responseDto)

        val service = UnitGroupService(unitGroupMapper, unitGroupCache, unitValueService, mock(OrgEntityAdvisoryLock::class.java))

        val result = service.createUnitGroup(insertDto)

        assertEquals(responseDto, result)
        verify(unitValueService, times(1)).ensurePieceUnitExists(groupId, "Boxes")
    }

    @Test
    fun `bulkCreateValidatedList does not provision a Piece unit itself`() {
        val unitGroupMapper = mock(UnitGroupMapper::class.java)
        val unitGroupCache = mock(UnitGroupCache::class.java)
        val unitValueService = mock(UnitValueService::class.java)

        val service = UnitGroupService(unitGroupMapper, unitGroupCache, unitValueService, mock(OrgEntityAdvisoryLock::class.java))

        service.bulkCreateValidatedList(emptyList())

        verifyNoInteractions(unitValueService)
    }

    @Test
    fun `createUnitGroup rejects a name equal to an existing group on alphanumerics only`() {
        val unitGroupCache = mock(UnitGroupCache::class.java)
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(listOf(unitGroupDto(UUID.randomUUID(), "Pack Size")))
        val service = UnitGroupService(mock(UnitGroupMapper::class.java), unitGroupCache, mock(UnitValueService::class.java), mock(OrgEntityAdvisoryLock::class.java))

        assertThrows(RtsGenericException::class.java) { service.createUnitGroup(UnitGroupInsertDto(name = "Pack-Size", description = null)) }
    }

    @Test
    fun `createUnitGroup rejects a name equal to a system group on alphanumerics only`() {
        val unitGroupCache = mock(UnitGroupCache::class.java)
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(listOf(unitGroupDto(UUID.randomUUID(), "Count")))
        val service = UnitGroupService(mock(UnitGroupMapper::class.java), unitGroupCache, mock(UnitValueService::class.java), mock(OrgEntityAdvisoryLock::class.java))

        assertThrows(RtsGenericException::class.java) { service.createUnitGroup(UnitGroupInsertDto(name = "Co-unt", description = null)) }
    }

    private fun unitGroupDto(id: UUID, name: String) = UnitGroupDto(
        id = id,
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-1",
        code = null,
        name = name,
        description = null,
        systemDefined = false
    )

    private fun unitGroupResponseDto(id: UUID, name: String) = UnitGroupResponseDto(
        id = id,
        createdBy = "Someone",
        createdOn = OffsetDateTime.now(),
        name = name,
        description = null,
        referenceNumber = "REF-1",
        systemDefined = false
    )
}
