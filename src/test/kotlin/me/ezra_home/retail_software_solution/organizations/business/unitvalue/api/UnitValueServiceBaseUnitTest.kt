package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueCache
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueEntity
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueMapper
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueValidator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class UnitValueServiceBaseUnitTest {

    private val unitGroupId = UUID.randomUUID()

    @ParameterizedTest
    @ValueSource(strings = ["Sachet", "Piece"])
    fun `ensureBaseUnitExists saves exactly the RootUnitDefinition for the group`(baseUnitName: String) {
        val savedEntity = provisionAndCaptureSavedEntity(baseUnitName, null)

        val rootUnitDefinition = RootUnitDefinition.forGroup(unitGroupId, unitGroupName = "Eggs", baseUnitName = baseUnitName, baseUnitCode = null)
        assertEquals(rootUnitDefinition.name, savedEntity.name)
        assertEquals(rootUnitDefinition.code, savedEntity.code)
        assertEquals(rootUnitDefinition.description, savedEntity.description)
        assertEquals(rootUnitDefinition.systemDefined, savedEntity.systemDefined)
        assertEquals(baseUnitName, savedEntity.name)
        assertEquals("eggs-base", savedEntity.code)
        assertNull(savedEntity.baseUnit)
    }

    @Test
    fun `ensureBaseUnitExists names the root unit Piece when no base unit name is given`() {
        val savedEntity = provisionAndCaptureSavedEntity(null, null)

        assertEquals("Piece", savedEntity.name)
        assertEquals("eggs-base", savedEntity.code)
    }

    @Test
    fun `ensureBaseUnitExists creates the root unit with the explicit base unit code`() {
        val savedEntity = provisionAndCaptureSavedEntity("Sachet", "egg-root")

        val rootUnitDefinition = RootUnitDefinition.forGroup(unitGroupId, unitGroupName = "Eggs", baseUnitName = "Sachet", baseUnitCode = "egg-root")
        assertEquals("egg-root", savedEntity.code)
        assertEquals(rootUnitDefinition.code, savedEntity.code)
        assertEquals(rootUnitDefinition.name, savedEntity.name)
        assertEquals(rootUnitDefinition.description, savedEntity.description)
        assertEquals(rootUnitDefinition.systemDefined, savedEntity.systemDefined)
        assertNull(savedEntity.baseUnit)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `ensureBaseUnitExists generates the root unit code when the base unit code is blank`(blankBaseUnitCode: String) {
        val savedEntity = provisionAndCaptureSavedEntity(null, blankBaseUnitCode)

        assertEquals("eggs-base", savedEntity.code)
    }

    private fun provisionAndCaptureSavedEntity(baseUnitName: String?, baseUnitCode: String?): UnitValueEntity {
        val unitValueCache = mock(UnitValueCache::class.java)
        var savedEntity: UnitValueEntity? = null
        `when`(unitValueCache.getByUnitGroupId(unitGroupId)).thenReturn(emptyList())
        `when`(unitValueCache.save(anyNonNull(UnitValueEntity::class.java))).thenAnswer { invocation ->
            savedEntity = invocation.getArgument(0)
            mock(UnitValueDto::class.java)
        }
        val unitValueService = UnitValueService(
            unitValueCache,
            mock(UnitValueMapper::class.java),
            mock(UnitValueValidator::class.java),
            mock(UnitConversionGraphFacade::class.java),
            mock(OrgEntityAdvisoryLock::class.java)
        )

        unitValueService.ensureBaseUnitExists(unitGroupId, unitGroupName = "Eggs", baseUnitName = baseUnitName, baseUnitCode = baseUnitCode)

        return savedEntity!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(type: Class<T>): T {
        any(type)
        return null as T
    }
}
