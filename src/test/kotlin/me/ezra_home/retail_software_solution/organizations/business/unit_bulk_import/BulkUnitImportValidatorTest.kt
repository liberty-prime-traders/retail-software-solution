package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.BulkUnitImportRequestDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitConversionBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitValueBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionService
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class BulkUnitImportValidatorTest {

    private lateinit var unitGroupService: UnitGroupService
    private lateinit var unitValueFetcher: UnitValueFetcher
    private lateinit var unitConversionService: UnitConversionService
    private lateinit var validator: BulkUnitImportValidator

    @BeforeEach
    fun setUp() {
        unitGroupService = mock(UnitGroupService::class.java)
        unitValueFetcher = mock(UnitValueFetcher::class.java)
        unitConversionService = mock(UnitConversionService::class.java)
        validator = BulkUnitImportValidator(unitGroupService, unitValueFetcher, unitConversionService)

        `when`(unitGroupService.getAllUnitGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())
        `when`(unitConversionService.getAll()).thenReturn(emptyList())
    }

    private fun unitValue(
        name: String? = "Half Dozen",
        code: String? = "hdz",
        baseUnitCode: String? = "__piece__",
        conversionFactor: Double? = 6.0
    ) = UnitValueBulkInsertDto(name = name, code = code, description = null, baseUnitCode = baseUnitCode, unitsOfBasePerUnit = conversionFactor)

    private fun group(
        name: String? = "Count",
        unitValues: List<UnitValueBulkInsertDto> = listOf(unitValue())
    ) = UnitGroupBulkInsertDto(name = name, description = null, unitValues = unitValues)

    @Test
    fun `valid payload produces no errors`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Count",
                    unitValues = listOf(
                        unitValue(name = "Half Dozen", code = "hdz", baseUnitCode = "__piece__", conversionFactor = 6.0),
                        unitValue(name = "Dozen", code = "dz", baseUnitCode = "__piece__", conversionFactor = 12.0)
                    )
                ),
                group(
                    name = "Nescafe",
                    unitValues = listOf(
                        unitValue(name = "Outer", code = "nescafe-out", baseUnitCode = null, conversionFactor = null),
                        unitValue(name = "Carton", code = "nescafe-ctn", baseUnitCode = "nescafe-out", conversionFactor = 6.0)
                    )
                ),
                group(
                    name = "Miscellaneous",
                    unitValues = listOf(
                        unitValue(name = "Generic Carton", code = "ctn", baseUnitCode = null, conversionFactor = null)
                    )
                )
            ),
            unitConversions = listOf(
                UnitConversionBulkInsertDto(fromUnitCode = "ctn", toUnitCode = "dz", unitsOfBasePerUnit = BigDecimal("42.0"))
            )
        )

        val errors = validator.validate(request)

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `duplicate group name within payload is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(group(name = "Count"), group(name = "Count", unitValues = listOf(unitValue(code = "dz2", name = "Dozen 2"))))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates another group name") })
    }

    @Test
    fun `group name with no letters or digits is rejected`() {
        val errors = validator.validate(BulkUnitImportRequestDto(unitGroups = listOf(group(name = "!!!"))))

        assertTrue(errors.any { it.contains("must contain at least one letter or digit") })
    }

    @Test
    fun `duplicate group name against the database is rejected`() {
        `when`(unitGroupService.getAllUnitGroupDtos()).thenReturn(
            listOf(
                UnitGroupDto(
                    id = UUID.randomUUID(),
                    createdById = UUID.randomUUID(),
                    createdOn = OffsetDateTime.now(),
                    referenceNumber = "REF-1",
                    code = null,
                    name = "Count",
                    description = null,
                    systemDefined = false
                )
            )
        )

        val errors = validator.validate(BulkUnitImportRequestDto(unitGroups = listOf(group(name = "Count"))))

        assertTrue(errors.any { it.contains("already exists in the database") })
    }

    @Test
    fun `base unit chain cycle across groups is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Nescafe",
                    unitValues = listOf(
                        unitValue(name = "Outer", code = "nescafe-out", baseUnitCode = "nescafe-ctn", conversionFactor = 1.0),
                        unitValue(name = "Carton", code = "nescafe-ctn", baseUnitCode = "nescafe-out", conversionFactor = 6.0)
                    )
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.startsWith("Cycle detected:") })
    }

    @Test
    fun `blank unit value codes do not collapse onto a phantom cycle-graph node`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Count",
                    unitValues = listOf(
                        unitValue(name = "First", code = "", baseUnitCode = null, conversionFactor = null),
                        unitValue(name = "Second", code = "", baseUnitCode = null, conversionFactor = null)
                    )
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("code is required") })
        assertTrue(errors.none { it.startsWith("Cycle detected:") })
    }

    @Test
    fun `baseUnitCode without unitsOfBasePerUnit is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(group(name = "Count", unitValues = listOf(unitValue(baseUnitCode = "__piece__", conversionFactor = null))))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("requires unitsOfBasePerUnit") })
    }

    @Test
    fun `piece base unit is rejected inside a Miscellaneous group`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Miscellaneous", unitValues = listOf(unitValue(name = "Carton", code = "ctn", baseUnitCode = "__piece__", conversionFactor = 6.0)))
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("is not valid inside the 'Miscellaneous' group") })
    }

    @Test
    fun `same unit value name is allowed across different groups`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Nescafe", unitValues = listOf(unitValue(name = "Carton", code = "nescafe-ctn", baseUnitCode = null, conversionFactor = null))),
                group(name = "Miscellaneous", unitValues = listOf(unitValue(name = "Carton", code = "ctn", baseUnitCode = null, conversionFactor = null)))
            )
        )

        val errors = validator.validate(request)

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `duplicate unit value name within the same group is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Count",
                    unitValues = listOf(
                        unitValue(name = "Half Dozen", code = "hdz", baseUnitCode = null, conversionFactor = null),
                        unitValue(name = "Half Dozen", code = "hdz2", baseUnitCode = null, conversionFactor = null)
                    )
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("name already exists in the 'Count' group") })
    }

    @Test
    fun `duplicate unit value code against the database is rejected even across different groups`() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(
            listOf(unitValueResponseDto(id = UUID.randomUUID(), code = "ctn", unitGroupId = UUID.randomUUID()))
        )
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(group(name = "Count", unitValues = listOf(unitValue(name = "Carton", code = "ctn", baseUnitCode = null, conversionFactor = null))))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("code already exists in the database") })
    }

    @Test
    fun `duplicate unit value code within the payload is rejected as a payload duplicate, not a database one`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Count",
                    unitValues = listOf(
                        unitValue(name = "Half Dozen", code = "hdz", baseUnitCode = null, conversionFactor = null),
                        unitValue(name = "Half Dozen 2", code = "hdz", baseUnitCode = null, conversionFactor = null)
                    )
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates another unit value code in the same payload") })
        assertTrue(errors.none { it.contains("code already exists in the database") })
    }

    @Test
    fun `conversion duplicated against an existing database conversion is rejected`() {
        val fromId = UUID.randomUUID()
        val toId = UUID.randomUUID()
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(
            listOf(
                unitValueResponseDto(id = fromId, code = "ctn", unitGroupId = UUID.randomUUID()),
                unitValueResponseDto(id = toId, code = "dz", unitGroupId = UUID.randomUUID())
            )
        )
        `when`(unitConversionService.getAll()).thenReturn(
            listOf(UnitConversionDto(id = UUID.randomUUID(), fromUnitId = fromId, toUnitId = toId, factor = BigDecimal("42.0")))
        )

        val request = BulkUnitImportRequestDto(
            unitGroups = emptyList(),
            unitConversions = listOf(UnitConversionBulkInsertDto(fromUnitCode = "ctn", toUnitCode = "dz", unitsOfBasePerUnit = BigDecimal("10.0")))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("already exists in the database") })
    }

    private fun unitValueResponseDto(id: UUID, code: String, unitGroupId: UUID) = UnitValueResponseDto(
        id = id,
        name = code,
        code = code,
        description = null,
        baseUnit = null,
        baseUnitName = null,
        conversionFactor = null,
        createdBy = "Someone",
        createdOn = OffsetDateTime.now(),
        unitGroupId = unitGroupId,
        referenceNumber = "REF-$code",
        systemDefined = false
    )
}
