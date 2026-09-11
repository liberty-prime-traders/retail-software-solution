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

        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                UnitGroupBulkInsertDto(
                    name = "Count",
                    description = null,
                    unitValues = listOf(
                        UnitValueBulkInsertDto(name = "Half Dozen", code = "hdz", description = null, baseUnitCode = "__piece__", unitsOfBasePerUnit = 6L),
                        UnitValueBulkInsertDto(name = "Dozen", code = "dz", description = null, baseUnitCode = "__piece__", unitsOfBasePerUnit = 12L)
                    )
                )
            )
        )
        val countGroupId = UUID.randomUUID()
        val groupInsertDtos = listOf(UnitGroupInsertDto(name = "Count", description = null))

        val savedGroups = listOf(unitGroupResponseDto(countGroupId, "Count"))

        `when`(bulkUnitImportValidator.validate(request)).thenReturn(emptyList())
        `when`(unitGroupService.bulkCreateValidatedList(groupInsertDtos)).thenReturn(savedGroups)
        `when`(unitValueFetcher.getUnitValuesForUnitGroup(countGroupId)).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())

        var savedEntries: List<UnitValueBulkSaveEntry> = emptyList()
        `when`(unitValueService.bulkCreateValidatedList(anyList<UnitValueBulkSaveEntry>())).thenAnswer { invocation ->
            savedEntries = invocation.getArgument(0)
            emptyList<UnitValueResponseDto>()
        }

        val service = BulkUnitImportService(
            bulkUnitImportValidator, unitGroupService, unitValueService, unitValueFetcher, unitConversionService
        )

        val result = service.bulkImport(request)

        assertEquals(savedGroups, result)
        assertEquals(3, savedEntries.size)

        val pieceEntry = savedEntries.first { it.code == "count-pc" }
        assertNull(pieceEntry.baseUnit)

        val pieceIndex = savedEntries.indexOf(pieceEntry)
        val hdzIndex = savedEntries.indexOfFirst { it.code == "hdz" }
        val dzIndex = savedEntries.indexOfFirst { it.code == "dz" }
        assert(pieceIndex < hdzIndex) { "Piece must be saved before Half Dozen" }
        assert(pieceIndex < dzIndex) { "Piece must be saved before Dozen" }

        assertEquals(pieceEntry.id, savedEntries.first { it.code == "hdz" }.baseUnit)
        assertEquals(pieceEntry.id, savedEntries.first { it.code == "dz" }.baseUnit)
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

    private fun unitValueResponseDto(id: UUID, code: String, unitGroupId: UUID) = UnitValueResponseDto(
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
        systemDefined = false
    )

    private fun unitGroupResponseDto(id: UUID, name: String?) = UnitGroupResponseDto(
        id = id,
        createdBy = "Someone",
        createdOn = OffsetDateTime.now(),
        name = name,
        description = null,
        referenceNumber = "REF-1",
        systemDefined = false
    )
}
