package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupMapper
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
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
        verify(unitValueService, times(1)).ensureBaseUnitExists(groupId, unitGroupName = "Boxes", baseUnitName = null, baseUnitCode = null)
    }

    @Test
    fun `createUnitGroup passes baseUnitName through to the base unit provisioning`() {
        val unitGroupMapper = mock(UnitGroupMapper::class.java)
        val unitGroupCache = mock(UnitGroupCache::class.java)
        val unitValueService = mock(UnitValueService::class.java)

        val insertDto = UnitGroupInsertDto(name = "Boxes", description = null, baseUnitName = "Sachet")
        val groupId = UUID.randomUUID()
        val groupDto = unitGroupDto(groupId, "Boxes")

        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(emptyList())
        `when`(unitGroupCache.create(insertDto)).thenReturn(groupDto)
        `when`(unitGroupMapper.toResponseDto(groupDto)).thenReturn(unitGroupResponseDto(groupId, "Boxes"))

        UnitGroupService(unitGroupMapper, unitGroupCache, unitValueService, mock(OrgEntityAdvisoryLock::class.java)).createUnitGroup(insertDto)

        verify(unitValueService, times(1)).ensureBaseUnitExists(groupId, unitGroupName = "Boxes", baseUnitName = "Sachet", baseUnitCode = null)
    }

    @Test
    fun `createUnitGroup passes baseUnitCode through to the base unit provisioning`() {
        val unitGroupMapper = mock(UnitGroupMapper::class.java)
        val unitGroupCache = mock(UnitGroupCache::class.java)
        val unitValueService = mock(UnitValueService::class.java)

        val insertDto = UnitGroupInsertDto(name = "Boxes", description = null, baseUnitName = "Sachet", baseUnitCode = "box-root")
        val groupId = UUID.randomUUID()
        val groupDto = unitGroupDto(groupId, "Boxes")

        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(emptyList())
        `when`(unitGroupCache.create(insertDto)).thenReturn(groupDto)
        `when`(unitGroupMapper.toResponseDto(groupDto)).thenReturn(unitGroupResponseDto(groupId, "Boxes"))

        UnitGroupService(unitGroupMapper, unitGroupCache, unitValueService, mock(OrgEntityAdvisoryLock::class.java)).createUnitGroup(insertDto)

        verify(unitValueService, times(1)).ensureBaseUnitExists(groupId, unitGroupName = "Boxes", baseUnitName = "Sachet", baseUnitCode = "box-root")
    }

    @Test
    fun `createUnitGroup validates the root unit code before saving the group`() {
        val unitGroupCache = mock(UnitGroupCache::class.java)
        val unitValueService = mock(UnitValueService::class.java)
        val insertDto = UnitGroupInsertDto(name = "Boxes", baseUnitCode = "taken-root")
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(emptyList())
        `when`(unitValueService.validateRootUnitCodeBeforeGroupCreate("Boxes", "taken-root")).thenThrow(RtsGenericException("taken"))
        val service = UnitGroupService(mock(UnitGroupMapper::class.java), unitGroupCache, unitValueService, mock(OrgEntityAdvisoryLock::class.java))

        assertThrows(RtsGenericException::class.java) { service.createUnitGroup(insertDto) }

        verify(unitGroupCache, never()).create(insertDto)
    }

    @Test
    fun `bulkCreateValidatedList does not provision a base unit itself`() {
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

    @Test
    fun `bulkCreateValidatedList rejects a name equal to an existing group before saving anything`() {
        val unitGroupCache = mock(UnitGroupCache::class.java)
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(listOf(unitGroupDto(UUID.randomUUID(), "Miscellaneous")))
        val service = UnitGroupService(mock(UnitGroupMapper::class.java), unitGroupCache, mock(UnitValueService::class.java), mock(OrgEntityAdvisoryLock::class.java))

        assertThrows(RtsGenericException::class.java) {
            service.bulkCreateValidatedList(listOf(UnitGroupInsertDto(name = "Boxes"), UnitGroupInsertDto(name = "miscellaneous")))
        }

        verify(unitGroupCache, never()).saveAll(anyList())
    }

    @Test
    fun `bulkCreateValidatedList rejects two names that match on alphanumerics`() {
        val unitGroupCache = mock(UnitGroupCache::class.java)
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(emptyList())
        val service = UnitGroupService(mock(UnitGroupMapper::class.java), unitGroupCache, mock(UnitValueService::class.java), mock(OrgEntityAdvisoryLock::class.java))

        assertThrows(RtsGenericException::class.java) {
            service.bulkCreateValidatedList(listOf(UnitGroupInsertDto(name = "Pack Size"), UnitGroupInsertDto(name = "Pack-Size")))
        }

        verify(unitGroupCache, never()).saveAll(anyList())
    }

    @Test
    fun `bulkCreateValidatedList takes the group name lock before reading the existing groups`() {
        val unitGroupCache = mock(UnitGroupCache::class.java)
        val unitGroupMapper = mock(UnitGroupMapper::class.java)
        val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)
        `when`(unitGroupCache.getAllUnitGroups()).thenReturn(emptyList())
        `when`(unitGroupCache.saveAll(anyList())).thenReturn(emptyList())
        val service = UnitGroupService(unitGroupMapper, unitGroupCache, mock(UnitValueService::class.java), orgEntityAdvisoryLock)

        service.bulkCreateValidatedList(listOf(UnitGroupInsertDto(name = "Boxes")))

        val order = inOrder(orgEntityAdvisoryLock, unitGroupCache)
        order.verify(orgEntityAdvisoryLock).acquire(LockNamespaces.UNIT_GROUP_NAME, "names")
        order.verify(unitGroupCache).getAllUnitGroups()
        order.verify(unitGroupCache).saveAll(anyList())
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
