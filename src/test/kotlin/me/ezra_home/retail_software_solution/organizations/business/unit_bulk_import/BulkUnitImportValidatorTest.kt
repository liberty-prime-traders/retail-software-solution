package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.BulkUnitImportRequestDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import.api.UnitValueBulkInsertDto
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
import java.time.OffsetDateTime
import java.util.UUID

class BulkUnitImportValidatorTest {

    private lateinit var unitGroupService: UnitGroupService
    private lateinit var unitValueFetcher: UnitValueFetcher
    private lateinit var validator: BulkUnitImportValidator

    @BeforeEach
    fun setUp() {
        unitGroupService = mock(UnitGroupService::class.java)
        unitValueFetcher = mock(UnitValueFetcher::class.java)
        validator = BulkUnitImportValidator(unitGroupService, unitValueFetcher)

        `when`(unitGroupService.getAllUnitGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())
    }

    private fun unitValue(
        name: String? = "Half Dozen",
        code: String? = "hdz",
        baseUnitCode: String? = "__piece__",
        unitsOfBasePerUnit: Long? = 6L
    ) = UnitValueBulkInsertDto(name = name, code = code, description = null, baseUnitCode = baseUnitCode, unitsOfBasePerUnit = unitsOfBasePerUnit)

    private fun group(
        name: String? = "Count",
        unitValues: List<UnitValueBulkInsertDto> = listOf(unitValue())
    ) = UnitGroupBulkInsertDto(name = name, description = null, unitValues = unitValues)

    private fun unitGroupDto(name: String, systemDefined: Boolean) = UnitGroupDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        referenceNumber = "REF-1",
        code = null,
        name = name,
        description = null,
        systemDefined = systemDefined
    )

    @Test
    fun `valid payload produces no errors`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                // A custom countable group, not the system "Count" group - "__piece__" auto-provisions
                // its own Piece here, which is only valid for a group that doesn't already have one.
                // Codes must not collide with SystemUnitValue's seeded codes ("hdz", "dz", "pc", ...)
                // - a real org already has those from UnitValueSeeder, so a colliding code would be
                // treated as a reuse of the existing system-defined row instead of a new one (see
                // this package's README, "A payload matching system-defined data is a reuse, not a
                // conflict").
                group(
                    name = "Eggs",
                    unitValues = listOf(
                        unitValue(name = "Half Dozen", code = "egg-hdz", baseUnitCode = "__piece__", unitsOfBasePerUnit = 6L),
                        unitValue(name = "Dozen", code = "egg-dz", baseUnitCode = "__piece__", unitsOfBasePerUnit = 12L)
                    )
                ),
                group(
                    name = "Nescafe",
                    unitValues = listOf(
                        unitValue(name = "Outer", code = "nescafe-out", baseUnitCode = "__piece__", unitsOfBasePerUnit = 1L),
                        unitValue(name = "Carton", code = "nescafe-ctn", baseUnitCode = "nescafe-out", unitsOfBasePerUnit = 6L)
                    )
                ),
                group(
                    name = "Miscellaneous",
                    unitValues = listOf(
                        unitValue(name = "Generic Carton", code = "ctn", baseUnitCode = null, unitsOfBasePerUnit = null)
                    )
                )
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
    fun `group name matching a system-defined group is reused, not rejected`() {
        `when`(unitGroupService.getAllUnitGroupDtos()).thenReturn(
            listOf(
                UnitGroupDto(
                    id = UUID.randomUUID(),
                    createdById = UUID.randomUUID(),
                    createdOn = OffsetDateTime.now(),
                    referenceNumber = "REF-1",
                    code = null,
                    name = "Weight",
                    description = null,
                    systemDefined = true
                )
            )
        )

        val errors = validator.validate(
            BulkUnitImportRequestDto(unitGroups = listOf(group(name = "Weight", unitValues = emptyList())))
        )

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `unit value code matching a system-defined unit value is reused, not rejected`() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(
            listOf(unitValueResponseDto(id = UUID.randomUUID(), code = "kg", unitGroupId = UUID.randomUUID(), systemDefined = true))
        )
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Weight", unitValues = listOf(unitValue(name = "Kilogram", code = "kg", baseUnitCode = null, unitsOfBasePerUnit = null)))
            )
        )

        val errors = validator.validate(request)

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `base unit chain cycle across groups is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Nescafe",
                    unitValues = listOf(
                        unitValue(name = "Outer", code = "nescafe-out", baseUnitCode = "nescafe-ctn", unitsOfBasePerUnit = 1L),
                        unitValue(name = "Carton", code = "nescafe-ctn", baseUnitCode = "nescafe-out", unitsOfBasePerUnit = 6L)
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
                        unitValue(name = "First", code = "", baseUnitCode = null, unitsOfBasePerUnit = null),
                        unitValue(name = "Second", code = "", baseUnitCode = null, unitsOfBasePerUnit = null)
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
            unitGroups = listOf(group(name = "Count", unitValues = listOf(unitValue(baseUnitCode = "__piece__", unitsOfBasePerUnit = null))))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("requires unitsOfBasePerUnit") })
    }

    @Test
    fun `piece base unit is rejected inside a Miscellaneous group`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Miscellaneous", unitValues = listOf(unitValue(name = "Carton", code = "ctn", baseUnitCode = "__piece__", unitsOfBasePerUnit = 6L)))
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("a Miscellaneous unit stands alone") })
    }

    @Test
    fun `same unit value name is allowed across different groups`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Nescafe", unitValues = listOf(unitValue(name = "Carton", code = "nescafe-ctn", baseUnitCode = "__piece__", unitsOfBasePerUnit = 6L))),
                group(name = "Miscellaneous", unitValues = listOf(unitValue(name = "Carton", code = "ctn", baseUnitCode = null, unitsOfBasePerUnit = null)))
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
                        unitValue(name = "Half Dozen", code = "hdz", baseUnitCode = null, unitsOfBasePerUnit = null),
                        unitValue(name = "Half Dozen", code = "hdz2", baseUnitCode = null, unitsOfBasePerUnit = null)
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
            unitGroups = listOf(group(name = "Count", unitValues = listOf(unitValue(name = "Carton", code = "ctn", baseUnitCode = null, unitsOfBasePerUnit = null))))
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
                        unitValue(name = "Half Dozen", code = "hdz", baseUnitCode = null, unitsOfBasePerUnit = null),
                        unitValue(name = "Half Dozen 2", code = "hdz", baseUnitCode = null, unitsOfBasePerUnit = null)
                    )
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates another unit value code in the same payload") })
        assertTrue(errors.none { it.contains("code already exists in the database") })
    }

    @Test
    fun `unit value code matching a system-defined unit value still flags a payload-internal duplicate`() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(
            listOf(unitValueResponseDto(id = UUID.randomUUID(), code = "kg", unitGroupId = UUID.randomUUID(), systemDefined = true))
        )
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Weight",
                    unitValues = listOf(
                        unitValue(name = "Kilogram", code = "kg", baseUnitCode = null, unitsOfBasePerUnit = null),
                        unitValue(name = "Kilogram 2", code = "kg", baseUnitCode = null, unitsOfBasePerUnit = null)
                    )
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates another unit value code in the same payload") })
    }

    @Test
    fun `a Miscellaneous unit with a base unit is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Miscellaneous", unitValues = listOf(unitValue(name = "Carton", code = "ctn", baseUnitCode = "pc", unitsOfBasePerUnit = 6L)))
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("a Miscellaneous unit stands alone") })
    }

    @Test
    fun `a unit outside Miscellaneous without a base unit is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Eggs", unitValues = listOf(unitValue(name = "Tray", code = "egg-tray", baseUnitCode = null, unitsOfBasePerUnit = null)))
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("needs a baseUnitCode") })
    }

    @Test
    fun `a payload unit named Piece in a custom group is rejected because the default root unit is automatic`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Eggs", unitValues = listOf(unitValue(name = "Piece", code = "egg-piece", baseUnitCode = "__piece__", unitsOfBasePerUnit = 1L)))
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("root unit 'Piece' is created automatically") && it.contains("__piece__") })
    }

    @Test
    fun `a payload unit named like a custom baseUnitName is rejected and the error names that base unit`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                UnitGroupBulkInsertDto(
                    name = "Eggs",
                    description = null,
                    baseUnitName = "Sachet",
                    unitValues = listOf(unitValue(name = "sa-chet", code = "egg-sachet", baseUnitCode = "__piece__", unitsOfBasePerUnit = 1L))
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("root unit 'Sachet' is created automatically") && it.contains("__piece__") })
    }

    @Test
    fun `a payload unit named Piece is accepted when the group renames its base unit`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                UnitGroupBulkInsertDto(
                    name = "Eggs",
                    description = null,
                    baseUnitName = "Sachet",
                    unitValues = listOf(unitValue(name = "Piece", code = "egg-piece", baseUnitCode = "__piece__", unitsOfBasePerUnit = 1L))
                )
            )
        )

        assertEquals(emptyList<String>(), validator.validate(request))
    }

    @Test
    fun `baseUnitName on a system group is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(UnitGroupBulkInsertDto(name = "Weight", description = null, baseUnitName = "Sachet", unitValues = emptyList()))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Group 'Weight'") && it.contains("baseUnitName is not allowed") })
    }

    @Test
    fun `baseUnitName on a payload group that reuses a system-defined group is rejected`() {
        `when`(unitGroupService.getAllUnitGroupDtos()).thenReturn(listOf(unitGroupDto("Warehouse Items", systemDefined = true)))
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(UnitGroupBulkInsertDto(name = "Warehouse Items", description = null, baseUnitName = "Sachet", unitValues = emptyList()))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Group 'Warehouse Items'") && it.contains("baseUnitName is not allowed") })
    }

    @Test
    fun `baseUnitName without a letter or digit is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(UnitGroupBulkInsertDto(name = "Eggs", description = null, baseUnitName = "--", unitValues = emptyList()))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("baseUnitName must contain at least one letter or digit") })
    }

    @Test
    fun `a blank baseUnitName on a system group is ignored`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(UnitGroupBulkInsertDto(name = "Weight", description = null, baseUnitName = " ", unitValues = emptyList()))
        )

        assertEquals(emptyList<String>(), validator.validate(request))
    }

    private fun groupWithBaseUnitCode(
        name: String,
        baseUnitCode: String?,
        unitValues: List<UnitValueBulkInsertDto> = emptyList()
    ) = UnitGroupBulkInsertDto(name = name, description = null, baseUnitCode = baseUnitCode, unitValues = unitValues)

    @Test
    fun `units referencing the root by the explicit baseUnitCode and by the piece marker are both accepted`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                groupWithBaseUnitCode(
                    name = "Eggs",
                    baseUnitCode = "egg-root",
                    unitValues = listOf(
                        unitValue(name = "Half Dozen", code = "egg-hdz", baseUnitCode = "egg-root", unitsOfBasePerUnit = 6L),
                        unitValue(name = "Dozen", code = "egg-dz", baseUnitCode = "__piece__", unitsOfBasePerUnit = 12L),
                        unitValue(name = "Crate", code = "egg-crate", baseUnitCode = "egg-dz", unitsOfBasePerUnit = 2L)
                    )
                )
            )
        )

        assertEquals(emptyList<String>(), validator.validate(request))
    }

    @Test
    fun `a chain size overflowing through the explicit root is still rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                groupWithBaseUnitCode(
                    name = "Eggs",
                    baseUnitCode = "egg-root",
                    unitValues = listOf(
                        unitValue(name = "Big", code = "egg-big", baseUnitCode = "egg-root", unitsOfBasePerUnit = 1_000_000L),
                        unitValue(name = "Bigger", code = "egg-bigger", baseUnitCode = "egg-big", unitsOfBasePerUnit = 1_000_000L)
                    )
                )
            )
        )

        assertTrue(validator.validate(request).any { it.contains("Unit chain rejected") })
    }

    @Test
    fun `a unit referencing another group's explicit root code is rejected as belonging to a different group`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                groupWithBaseUnitCode(name = "Eggs", baseUnitCode = "egg-root"),
                groupWithBaseUnitCode(
                    name = "Milk",
                    baseUnitCode = "milk-root",
                    unitValues = listOf(unitValue(name = "Carton", code = "milk-carton", baseUnitCode = "egg-root", unitsOfBasePerUnit = 4L))
                )
            )
        )

        assertTrue(validator.validate(request).any { it.contains("baseUnitCode 'egg-root' belongs to a different unit group") })
    }

    @Test
    fun `baseUnitCode of the piece marker is rejected`() {
        val request = BulkUnitImportRequestDto(unitGroups = listOf(groupWithBaseUnitCode("Eggs", "__piece__")))

        assertTrue(validator.validate(request).any { it.contains("Group 'Eggs'") && it.contains("is reserved") })
    }

    @Test
    fun `baseUnitCode longer than a unit value code is rejected`() {
        val request = BulkUnitImportRequestDto(unitGroups = listOf(groupWithBaseUnitCode("Eggs", "a".repeat(26))))

        assertTrue(validator.validate(request).any { it.contains("Group 'Eggs'") && it.contains("baseUnitCode must be at most 25 characters") })
    }

    @Test
    fun `baseUnitCode duplicating a payload unit value code is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                groupWithBaseUnitCode(
                    name = "Eggs",
                    baseUnitCode = "milk-carton",
                    unitValues = emptyList()
                ),
                groupWithBaseUnitCode(
                    name = "Milk",
                    baseUnitCode = null,
                    unitValues = listOf(unitValue(name = "Carton", code = "milk-carton", baseUnitCode = "__piece__", unitsOfBasePerUnit = 4L))
                )
            )
        )

        assertTrue(validator.validate(request).any { it.contains("Group 'Eggs'") && it.contains("duplicates a unit value code in the same payload") })
    }

    @Test
    fun `baseUnitCode duplicating another group's baseUnitCode is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(groupWithBaseUnitCode("Eggs", "shared-root"), groupWithBaseUnitCode("Milk", "shared-root"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Group 'Milk'") && it.contains("duplicates another group's base unit code") })
        assertTrue(errors.none { it.contains("Group 'Eggs'") })
    }

    @Test
    fun `baseUnitCode equal to another group's generated base unit code is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(groupWithBaseUnitCode("Eggs", null), groupWithBaseUnitCode("Milk", "eggs-base"))
        )

        assertTrue(validator.validate(request).any { it.contains("Group 'Milk'") && it.contains("duplicates another group's base unit code") })
    }

    @Test
    fun `baseUnitCode matching an existing database unit code is rejected`() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueResponseDto(UUID.randomUUID(), "taken", UUID.randomUUID())))
        val request = BulkUnitImportRequestDto(unitGroups = listOf(groupWithBaseUnitCode("Eggs", "taken")))

        assertTrue(validator.validate(request).any { it.contains("Group 'Eggs'") && it.contains("baseUnitCode 'taken' already exists in the database") })
    }

    @Test
    fun `baseUnitCode on a system group is rejected`() {
        val request = BulkUnitImportRequestDto(unitGroups = listOf(groupWithBaseUnitCode("Weight", "weight-root")))

        assertTrue(validator.validate(request).any { it.contains("Group 'Weight'") && it.contains("baseUnitCode is not allowed") })
    }

    @Test
    fun `baseUnitCode on a payload group that reuses a system-defined group is rejected`() {
        `when`(unitGroupService.getAllUnitGroupDtos()).thenReturn(listOf(unitGroupDto("Warehouse Items", systemDefined = true)))
        val request = BulkUnitImportRequestDto(unitGroups = listOf(groupWithBaseUnitCode("Warehouse Items", "wh-root")))

        assertTrue(validator.validate(request).any { it.contains("Group 'Warehouse Items'") && it.contains("baseUnitCode is not allowed") })
    }

    @Test
    fun `a blank baseUnitCode on a system group is ignored`() {
        val request = BulkUnitImportRequestDto(unitGroups = listOf(groupWithBaseUnitCode("Weight", " ")))

        assertEquals(emptyList<String>(), validator.validate(request))
    }

    @Test
    fun `the generated code collision check is skipped for groups that supply a baseUnitCode`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                groupWithBaseUnitCode("A very long group name one", "long-one-root"),
                groupWithBaseUnitCode("A very long group name two", "long-two-root")
            )
        )

        assertEquals(emptyList<String>(), validator.validate(request))
    }

    @Test
    fun `group names equal on alphanumerics are rejected as duplicates`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Pack Size", unitValues = emptyList()),
                group(name = "Pack-Size", unitValues = emptyList())
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates another group name") })
    }

    @Test
    fun `two long group names sharing a base unit code slug are rejected with a clear error`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "A very long group name one", unitValues = emptyList()),
                group(name = "A very long group name two", unitValues = emptyList())
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("generated base unit code") })
    }

    @Test
    fun `unitsOfBasePerUnit above the size cap is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(name = "Eggs", unitValues = listOf(unitValue(name = "Pallet", code = "egg-pallet", baseUnitCode = "__piece__", unitsOfBasePerUnit = 1_000_000_001L)))
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("1,000,000,000") })
    }

    @Test
    fun `a chain whose size in the root exceeds the cap is rejected`() {
        val request = BulkUnitImportRequestDto(
            unitGroups = listOf(
                group(
                    name = "Eggs",
                    unitValues = listOf(
                        unitValue(name = "Tray", code = "egg-tray", baseUnitCode = "__piece__", unitsOfBasePerUnit = 100_000L),
                        unitValue(name = "Pallet", code = "egg-pallet", baseUnitCode = "egg-tray", unitsOfBasePerUnit = 100_000L)
                    )
                )
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.startsWith("Unit chain rejected:") })
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
}
