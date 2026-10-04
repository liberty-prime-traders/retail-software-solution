package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.BulkUnitImportValidator
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionService
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueBulkSaveEntry
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class BulkUnitImportServiceTest {

    @Test
    fun `saveUnitValues auto-inserts Piece once per referencing group and orders parents before children`() {
        val bulkUnitImportValidator = mock(BulkUnitImportValidator::class.java)
        val unitGroupService = mock(UnitGroupService::class.java)
        val unitValueService = mock(UnitValueService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)
        val unitConversionService = mock(UnitConversionService::class.java)

        // A custom countable group, not the system "Count" group - only a custom group gets an
        // auto-inserted Piece, since the system "Count" group already has the real one. Codes must
        // not collide with SystemUnitValue's seeded codes ("hdz", "dz", "pc", ...) - a real org
        // already has those from UnitValueSeeder, so a colliding code would be treated as a reuse
        // of the existing system-defined row instead of creating a new one (see this package's
        // README, "A payload matching system-defined data is a reuse, not a conflict").
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                UnitGroupBulkInsertDto(
                    name = "Eggs",
                    description = null,
                    unitValues = listOf(
                        UnitValueBulkInsertDto(name = "Half Dozen", code = "egg-hdz", description = null, baseUnitCode = "__piece__", unitsOfBasePerUnit = 6L),
                        UnitValueBulkInsertDto(name = "Dozen", code = "egg-dz", description = null, baseUnitCode = "__piece__", unitsOfBasePerUnit = 12L)
                    )
                )
            )
        )
        val eggsGroupId = UUID.randomUUID()
        val groupInsertDtos = listOf(UnitGroupInsertDto(name = "Eggs", description = null))

        val savedGroups = listOf(unitGroupResponseDto(eggsGroupId, "Eggs"))

        `when`(bulkUnitImportValidator.validate(request)).thenReturn(emptyList())
        `when`(unitGroupService.bulkCreateValidatedList(groupInsertDtos)).thenReturn(savedGroups)
        `when`(unitValueFetcher.getUnitValuesForUnitGroup(eggsGroupId)).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())

        var savedEntries: List<UnitValueBulkSaveEntry> = emptyList()
        `when`(unitValueService.bulkCreateValidatedList(anyList())).thenAnswer { invocation ->
            savedEntries = invocation.getArgument(0)
            emptyList<UnitValueResponseDto>()
        }

        val service = BulkUnitImportService(
            bulkUnitImportValidator, unitGroupService, unitValueService, unitValueFetcher, unitConversionService
        )

        val result = service.bulkImport(request)

        assertEquals(savedGroups, result)
        assertEquals(3, savedEntries.size)

        val pieceEntry = savedEntries.first { it.code == "eggs-pc" }
        assertNull(pieceEntry.baseUnit)

        val pieceIndex = savedEntries.indexOf(pieceEntry)
        val hdzIndex = savedEntries.indexOfFirst { it.code == "egg-hdz" }
        val dzIndex = savedEntries.indexOfFirst { it.code == "egg-dz" }
        assert(pieceIndex < hdzIndex) { "Piece must be saved before Half Dozen" }
        assert(pieceIndex < dzIndex) { "Piece must be saved before Dozen" }

        assertEquals(pieceEntry.id, savedEntries.first { it.code == "egg-hdz" }.baseUnit)
        assertEquals(pieceEntry.id, savedEntries.first { it.code == "egg-dz" }.baseUnit)
    }

    @Test
    fun `bulkImport reuses a system-defined group and unit value instead of recreating them`() {
        val bulkUnitImportValidator = mock(BulkUnitImportValidator::class.java)
        val unitGroupService = mock(UnitGroupService::class.java)
        val unitValueService = mock(UnitValueService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)
        val unitConversionService = mock(UnitConversionService::class.java)

        val weightGroupId = UUID.randomUUID()
        val kilogramId = UUID.randomUUID()
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                UnitGroupBulkInsertDto(
                    name = "Weight",
                    description = null,
                    unitValues = listOf(
                        UnitValueBulkInsertDto(name = "Kilogram", code = "kg", description = null, baseUnitCode = null, unitsOfBasePerUnit = null),
                        UnitValueBulkInsertDto(name = "Sack", code = "sack", description = null, baseUnitCode = "kg", unitsOfBasePerUnit = 25L)
                    )
                )
            )
        )

        val existingWeightGroup = unitGroupResponseDto(weightGroupId, "Weight", systemDefined = true)
        `when`(bulkUnitImportValidator.validate(request)).thenReturn(emptyList())
        `when`(unitGroupService.getAllUnitGroups()).thenReturn(listOf(existingWeightGroup))
        `when`(unitGroupService.bulkCreateValidatedList(emptyList())).thenReturn(emptyList())
        `when`(unitValueFetcher.getUnitValuesForUnitGroup(weightGroupId)).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(
            listOf(unitValueResponseDto(id = kilogramId, code = "kg", unitGroupId = weightGroupId, systemDefined = true))
        )

        var savedEntries: List<UnitValueBulkSaveEntry> = emptyList()
        `when`(unitValueService.bulkCreateValidatedList(anyList<UnitValueBulkSaveEntry>())).thenAnswer { invocation ->
            savedEntries = invocation.getArgument(0)
            emptyList<UnitValueResponseDto>()
        }

        val service = BulkUnitImportService(
            bulkUnitImportValidator, unitGroupService, unitValueService, unitValueFetcher, unitConversionService
        )

        val result = service.bulkImport(request)

        assertEquals(listOf(existingWeightGroup), result)
        assertEquals(1, savedEntries.size)
        assertEquals("sack", savedEntries.first().code)
        assertEquals(kilogramId, savedEntries.first().baseUnit)
    }

    @Test
    fun `bulkImport throws with the full error list and saves nothing when validation fails`() {
        val bulkUnitImportValidator = mock(BulkUnitImportValidator::class.java)
        val unitGroupService = mock(UnitGroupService::class.java)
        val unitValueService = mock(UnitValueService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)
        val unitConversionService = mock(UnitConversionService::class.java)

        val request = BulkUnitImportRequestDto(unitGroups = emptyList())
        val validationErrors = listOf("Group at position 1: name is required")
        `when`(bulkUnitImportValidator.validate(request)).thenReturn(validationErrors)

        val service = BulkUnitImportService(
            bulkUnitImportValidator, unitGroupService, unitValueService, unitValueFetcher, unitConversionService
        )

        val exception = assertThrows(RtsGenericException::class.java) { service.bulkImport(request) }

        assertEquals(validationErrors, exception.payload)
        verifyNoInteractions(unitGroupService, unitValueService, unitConversionService)
    }

    @Test
    fun `saveConversions resolves codes to ids and passes numerator and denominator through`() {
        val bulkUnitImportValidator = mock(BulkUnitImportValidator::class.java)
        val unitGroupService = mock(UnitGroupService::class.java)
        val unitValueService = mock(UnitValueService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)
        val unitConversionService = mock(UnitConversionService::class.java)

        val fromId = UUID.randomUUID()
        val toId = UUID.randomUUID()
        val request = BulkUnitImportRequestDto(
            unitGroups = emptyList(),
            unitConversions = listOf(UnitConversionBulkInsertDto(fromUnitCode = "ctn", toUnitCode = "dz", numerator = 42L, denominator = 1L))
        )

        `when`(bulkUnitImportValidator.validate(request)).thenReturn(emptyList())
        `when`(unitGroupService.bulkCreateValidatedList(emptyList())).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(
            listOf(
                unitValueResponseDto(id = fromId, code = "ctn", unitGroupId = UUID.randomUUID()),
                unitValueResponseDto(id = toId, code = "dz", unitGroupId = UUID.randomUUID())
            )
        )

        var insertedConversions: List<UnitConversionInsertDto> = emptyList()
        `when`(unitConversionService.bulkInsertValidatedList(anyList<UnitConversionInsertDto>())).thenAnswer { invocation ->
            insertedConversions = invocation.getArgument(0)
            emptyList<UnitConversionDto>()
        }

        val service = BulkUnitImportService(
            bulkUnitImportValidator, unitGroupService, unitValueService, unitValueFetcher, unitConversionService
        )

        service.bulkImport(request)

        assertEquals(1, insertedConversions.size)
        val insertDto = insertedConversions.first()
        assertEquals(fromId, insertDto.fromUnitId)
        assertEquals(toId, insertDto.toUnitId)
        assertEquals(42L, insertDto.numerator)
        assertEquals(1L, insertDto.denominator)
    }

    private fun unitValueResponseDto(id: UUID, code: String, unitGroupId: UUID, systemDefined: Boolean = false) = UnitValueResponseDto(
        id = id,
        name = code,
        code = code,
        description = null,
        baseUnit = null,
        baseUnitName = null,
        unitsOfBasePerUnit = null,
        createdBy = "Someone",
        createdOn = OffsetDateTime.now(),
        unitGroupId = unitGroupId,
        referenceNumber = "REF-$code",
        systemDefined = systemDefined
    )

    private fun unitGroupResponseDto(id: UUID, name: String, systemDefined: Boolean = false) = UnitGroupResponseDto(
        id = id,
        createdBy = "Someone",
        createdOn = OffsetDateTime.now(),
        name = name,
        description = null,
        referenceNumber = "REF-1",
        systemDefined = systemDefined
    )
}
