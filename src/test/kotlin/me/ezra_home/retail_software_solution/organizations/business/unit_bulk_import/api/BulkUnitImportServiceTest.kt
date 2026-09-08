package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.BulkUnitImportValidator
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionService
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueBulkSaveEntry
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
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
                        UnitValueBulkInsertDto(name = "Half Dozen", code = "hdz", description = null, baseUnitCode = "__piece__", unitsOfBasePerUnit = 6.0),
                        UnitValueBulkInsertDto(name = "Dozen", code = "dz", description = null, baseUnitCode = "__piece__", unitsOfBasePerUnit = 12.0)
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
