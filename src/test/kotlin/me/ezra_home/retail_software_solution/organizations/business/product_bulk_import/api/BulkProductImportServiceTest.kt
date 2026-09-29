package me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api

import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.BulkProductImportValidator
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryService
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupResponseDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
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

class BulkProductImportServiceTest {

    private fun categoryDto(id: UUID, name: String) = ProductCategoryDto(
        id = id, createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(), referenceNumber = "ref", categoryName = name, description = null
    )

    private fun categoryResponseDto(id: UUID, name: String) = ProductCategoryResponseDto(
        id = id, categoryName = name, description = null, createdBy = "someone", createdOn = OffsetDateTime.now(), referenceNumber = "ref"
    )

    private fun groupDto(id: UUID, categoryId: UUID, name: String) = ProductGroupDto(
        id = id, createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(), referenceNumber = "ref", groupName = name, description = null, categoryId = categoryId
    )

    private fun groupResponseDto(id: UUID, categoryId: UUID, name: String) = ProductGroupResponseDto(
        id = id, groupName = name, description = null, categoryId = categoryId, categoryName = null,
        createdBy = "someone", createdOn = OffsetDateTime.now(), referenceNumber = "ref"
    )

    private fun productResponseDto(id: UUID, name: String) = OrganizationProductResponseDto(
        id = id, productName = name, description = null, categoryName = null, categoryId = null, productGroupId = UUID.randomUUID(),
        productGroupName = "group", createdBy = "someone", createdOn = OffsetDateTime.now(), baseUnit = "unit", baseUnitId = UUID.randomUUID(),
        status = ProductStatus.ACTIVE, activeTags = null, referenceNumber = "ref"
    )

    private fun unitValueDto(id: UUID, code: String) = UnitValueResponseDto(
        id = id, name = code, code = code, description = null, baseUnit = null, baseUnitName = null, unitsOfBasePerUnit = null,
        createdBy = "someone", createdOn = OffsetDateTime.now(), unitGroupId = UUID.randomUUID(), referenceNumber = "ref", systemDefined = false
    )

    @Test
    fun `bulkImport resolves categoryId, groupId, and baseUnitId and saves each entity type in a single batch`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val request = BulkProductImportRequestDto(
            categories = listOf(ProductCategoryBulkInsertDto(name = "Beverages")),
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages")),
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = "Soft Drinks", unitCode = "jc-jug"))
        )

        val categoryId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val unitValueId = UUID.randomUUID()

        `when`(bulkProductImportValidator.validate(request)).thenReturn(emptyList())
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(emptyList())
        `when`(productGroupService.getAllGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueDto(unitValueId, "jc-jug")))
        `when`(productCategoryService.bulkCreateValidatedList(listOf(ProductCategoryInsertDto(categoryName = "Beverages", description = null))))
            .thenReturn(listOf(categoryResponseDto(categoryId, "Beverages")))
        `when`(productGroupService.bulkCreateValidatedList(
            listOf(ProductGroupInsertDto(groupName = "Soft Drinks", description = null, categoryId = categoryId))
        )).thenReturn(listOf(groupResponseDto(groupId, categoryId, "Soft Drinks")))
        `when`(organizationProductService.bulkCreateValidatedList(
            listOf(OrganizationProductInsertDto(productName = "Juice Cola", description = null, productGroupId = groupId, baseUnitId = unitValueId))
        )).thenReturn(listOf(productResponseDto(UUID.randomUUID(), "Juice Cola")))

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        service.bulkImport(request)

        verify(productCategoryService).bulkCreateValidatedList(listOf(ProductCategoryInsertDto(categoryName = "Beverages", description = null)))
        verify(productGroupService).bulkCreateValidatedList(listOf(ProductGroupInsertDto(groupName = "Soft Drinks", description = null, categoryId = categoryId)))
        verify(organizationProductService).bulkCreateValidatedList(
            listOf(OrganizationProductInsertDto(productName = "Juice Cola", description = null, productGroupId = groupId, baseUnitId = unitValueId))
        )
    }

    @Test
    fun `bulkImport reuses an existing category and group instead of recreating them`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val request = BulkProductImportRequestDto(
            categories = listOf(ProductCategoryBulkInsertDto(name = "Beverages")),
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages"))
        )

        val categoryId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        `when`(bulkProductImportValidator.validate(request)).thenReturn(emptyList())
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(categoryDto(categoryId, "Beverages")))
        `when`(productGroupService.getAllGroupDtos()).thenReturn(listOf(groupDto(groupId, categoryId, "Soft Drinks")))
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        service.bulkImport(request)

        verify(productCategoryService).bulkCreateValidatedList(emptyList())
        verify(productGroupService).bulkCreateValidatedList(emptyList())
    }

    @Test
    fun `bulkImport only saves the category and group that do not already exist, resolving the new group's categoryId to the reused category`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val request = BulkProductImportRequestDto(
            categories = listOf(ProductCategoryBulkInsertDto(name = "Beverages"), ProductCategoryBulkInsertDto(name = "Snacks")),
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages"))
        )

        val existingCategoryId = UUID.randomUUID()
        val newCategoryId = UUID.randomUUID()
        val newGroupId = UUID.randomUUID()

        `when`(bulkProductImportValidator.validate(request)).thenReturn(emptyList())
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(categoryDto(existingCategoryId, "Beverages")))
        `when`(productGroupService.getAllGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())
        `when`(productCategoryService.bulkCreateValidatedList(listOf(ProductCategoryInsertDto(categoryName = "Snacks", description = null))))
            .thenReturn(listOf(categoryResponseDto(newCategoryId, "Snacks")))
        `when`(productGroupService.bulkCreateValidatedList(
            listOf(ProductGroupInsertDto(groupName = "Soft Drinks", description = null, categoryId = existingCategoryId))
        )).thenReturn(listOf(groupResponseDto(newGroupId, existingCategoryId, "Soft Drinks")))

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        service.bulkImport(request)

        verify(productCategoryService).bulkCreateValidatedList(listOf(ProductCategoryInsertDto(categoryName = "Snacks", description = null)))
        verify(productGroupService).bulkCreateValidatedList(
            listOf(ProductGroupInsertDto(groupName = "Soft Drinks", description = null, categoryId = existingCategoryId))
        )
    }

    @Test
    fun `bulkImport with an empty payload saves nothing for any entity type`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val request = BulkProductImportRequestDto()

        `when`(bulkProductImportValidator.validate(request)).thenReturn(emptyList())
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(emptyList())
        `when`(productGroupService.getAllGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        service.bulkImport(request)

        verify(productCategoryService).bulkCreateValidatedList(emptyList())
        verify(productGroupService).bulkCreateValidatedList(emptyList())
        verify(organizationProductService).bulkCreateValidatedList(emptyList())
    }

    @Test
    fun `bulkImport batches multiple new categories, groups under a brand-new category, and products under a brand-new group into one call each`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val request = BulkProductImportRequestDto(
            categories = listOf(
                ProductCategoryBulkInsertDto(name = "Beverages"),
                ProductCategoryBulkInsertDto(name = "Snacks"),
                ProductCategoryBulkInsertDto(name = "Frozen")
            ),
            productGroups = listOf(
                ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Snacks"),
                ProductGroupBulkInsertDto(name = "Chips", categoryName = "Snacks")
            ),
            products = listOf(
                ProductBulkInsertDto(name = "Cola", groupName = "Soft Drinks", unitCode = "u1"),
                ProductBulkInsertDto(name = "Juice", groupName = "Soft Drinks", unitCode = "u1")
            )
        )

        val existingCategoryId = UUID.randomUUID()
        val snacksId = UUID.randomUUID()
        val frozenId = UUID.randomUUID()
        val softDrinksGroupId = UUID.randomUUID()
        val chipsGroupId = UUID.randomUUID()
        val unitId = UUID.randomUUID()

        `when`(bulkProductImportValidator.validate(request)).thenReturn(emptyList())
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(categoryDto(existingCategoryId, "Beverages")))
        `when`(productGroupService.getAllGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueDto(unitId, "u1")))

        val newCategoryInsertDtos = listOf(
            ProductCategoryInsertDto(categoryName = "Snacks", description = null),
            ProductCategoryInsertDto(categoryName = "Frozen", description = null)
        )
        `when`(productCategoryService.bulkCreateValidatedList(newCategoryInsertDtos))
            .thenReturn(listOf(categoryResponseDto(snacksId, "Snacks"), categoryResponseDto(frozenId, "Frozen")))

        val newGroupInsertDtos = listOf(
            ProductGroupInsertDto(groupName = "Soft Drinks", description = null, categoryId = snacksId),
            ProductGroupInsertDto(groupName = "Chips", description = null, categoryId = snacksId)
        )
        `when`(productGroupService.bulkCreateValidatedList(newGroupInsertDtos))
            .thenReturn(listOf(groupResponseDto(softDrinksGroupId, snacksId, "Soft Drinks"), groupResponseDto(chipsGroupId, snacksId, "Chips")))

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        service.bulkImport(request)

        // Frozen has no group and is never referenced, but it still must go through in the same single batch as Snacks.
        verify(productCategoryService, times(1)).bulkCreateValidatedList(newCategoryInsertDtos)
        verify(productGroupService, times(1)).bulkCreateValidatedList(newGroupInsertDtos)
        verify(organizationProductService, times(1)).bulkCreateValidatedList(
            listOf(
                OrganizationProductInsertDto(productName = "Cola", description = null, productGroupId = softDrinksGroupId, baseUnitId = unitId),
                OrganizationProductInsertDto(productName = "Juice", description = null, productGroupId = softDrinksGroupId, baseUnitId = unitId)
            )
        )
    }

    @Test
    fun `bulkImport reuses an existing category matched only by case and whitespace, not just an exact name match`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val existingCategoryId = UUID.randomUUID()
        val request = BulkProductImportRequestDto(categories = listOf(ProductCategoryBulkInsertDto(name = "Beverages")))

        `when`(bulkProductImportValidator.validate(request)).thenReturn(emptyList())
        // Stored name has different case and extra whitespace from what the payload sends.
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(categoryDto(existingCategoryId, "  BEVERAGES ")))
        `when`(productGroupService.getAllGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        service.bulkImport(request)

        // Proves the service's own StringUtils.normalizeForComparison map keys agree with the validator's
        // StringUtils.isEquivalent dedup check — if they ever drifted apart, this category would be recreated.
        verify(productCategoryService).bulkCreateValidatedList(emptyList())
    }

    @Test
    fun `bulkImport reuses an existing group even when the payload's categoryName does not match the group's actual category`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val beveragesId = UUID.randomUUID()
        val snacksId = UUID.randomUUID()
        val existingGroupId = UUID.randomUUID()

        // "Soft Drinks" already exists under Beverages, but this payload row claims it belongs to Snacks.
        val request = BulkProductImportRequestDto(
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Snacks"))
        )

        `when`(bulkProductImportValidator.validate(request)).thenReturn(emptyList())
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(categoryDto(beveragesId, "Beverages"), categoryDto(snacksId, "Snacks")))
        `when`(productGroupService.getAllGroupDtos()).thenReturn(listOf(groupDto(existingGroupId, beveragesId, "Soft Drinks")))
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        service.bulkImport(request)

        // Group reuse matches on name only; it never re-parents an existing group to a different payload categoryName.
        verify(productGroupService).bulkCreateValidatedList(emptyList())
    }

    @Test
    fun `bulkImport throws with the validation errors and never saves when validation fails`() {
        val bulkProductImportValidator = mock(BulkProductImportValidator::class.java)
        val productCategoryService = mock(ProductCategoryService::class.java)
        val productGroupService = mock(ProductGroupService::class.java)
        val organizationProductService = mock(OrganizationProductService::class.java)
        val unitValueFetcher = mock(UnitValueFetcher::class.java)

        val request = BulkProductImportRequestDto(categories = listOf(ProductCategoryBulkInsertDto(name = null)))
        val validationErrors = listOf("Category at position 1: name is required")
        `when`(bulkProductImportValidator.validate(request)).thenReturn(validationErrors)

        val service = BulkProductImportService(
            bulkProductImportValidator, productCategoryService, productGroupService, organizationProductService, unitValueFetcher
        )

        val exception = assertThrows(RtsGenericException::class.java) { service.bulkImport(request) }

        assertEquals(validationErrors, exception.payload)
        verifyNoInteractions(productCategoryService, productGroupService, organizationProductService, unitValueFetcher)
    }
}
