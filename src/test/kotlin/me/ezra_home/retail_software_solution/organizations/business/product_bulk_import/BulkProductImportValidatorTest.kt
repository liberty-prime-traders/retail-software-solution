package me.ezra_home.retail_software_solution.organizations.business.product_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.BulkProductImportRequestDto
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.ProductBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.ProductCategoryBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.ProductGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryService
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class BulkProductImportValidatorTest {

    private lateinit var productCategoryService: ProductCategoryService
    private lateinit var productGroupService: ProductGroupService
    private lateinit var organizationProductService: OrganizationProductService
    private lateinit var unitValueFetcher: UnitValueFetcher
    private lateinit var validator: BulkProductImportValidator

    @BeforeEach
    fun setUp() {
        productCategoryService = mock(ProductCategoryService::class.java)
        productGroupService = mock(ProductGroupService::class.java)
        organizationProductService = mock(OrganizationProductService::class.java)
        unitValueFetcher = mock(UnitValueFetcher::class.java)
        validator = BulkProductImportValidator(productCategoryService, productGroupService, organizationProductService, unitValueFetcher)

        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(emptyList())
        `when`(productGroupService.getAllGroupDtos()).thenReturn(emptyList())
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(emptyList())
        `when`(organizationProductService.isDuplicateProductName(org.mockito.ArgumentMatchers.anyString())).thenReturn(false)
    }

    private fun categoryDto(name: String) = ProductCategoryDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(),
        referenceNumber = "ref", categoryName = name, description = null
    )

    private fun groupDto(name: String, categoryId: UUID) = ProductGroupDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(),
        referenceNumber = "ref", groupName = name, description = null, categoryId = categoryId
    )

    private fun unitValueDto(code: String) = UnitValueResponseDto(
        id = UUID.randomUUID(), name = code, code = code, description = null, baseUnit = null, baseUnitName = null,
        unitsOfBasePerUnit = null, createdBy = "someone", createdOn = OffsetDateTime.now(),
        unitGroupId = UUID.randomUUID(), referenceNumber = "ref", systemDefined = false
    )

    @Test
    fun `valid payload produces no errors`() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueDto("jc-jug")))

        val request = BulkProductImportRequestDto(
            categories = listOf(ProductCategoryBulkInsertDto(name = "Beverages")),
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages")),
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = "Soft Drinks", unitCode = "jc-jug"))
        )

        val errors = validator.validate(request)

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `category without a name is reported`() {
        val request = BulkProductImportRequestDto(categories = listOf(ProductCategoryBulkInsertDto(name = null)))

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("name is required") })
    }

    @Test
    fun `duplicate category name within the payload is reported`() {
        val request = BulkProductImportRequestDto(
            categories = listOf(ProductCategoryBulkInsertDto(name = "Beverages"), ProductCategoryBulkInsertDto(name = "beverages"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates category name") })
    }

    @Test
    fun `product group with an unresolvable categoryName is reported`() {
        val request = BulkProductImportRequestDto(
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Nonexistent"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("categoryName 'Nonexistent'") })
    }

    @Test
    fun `product group can resolve categoryName against an existing category`() {
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(categoryDto("Beverages")))
        val request = BulkProductImportRequestDto(
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages"))
        )

        val errors = validator.validate(request)

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `product with a name that already exists in the DB is reported`() {
        `when`(organizationProductService.isDuplicateProductName("Juice Cola")).thenReturn(true)
        val existingCategory = categoryDto("Beverages")
        `when`(productGroupService.getAllGroupDtos()).thenReturn(listOf(groupDto("Soft Drinks", existingCategory.id)))
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueDto("jc-jug")))

        val request = BulkProductImportRequestDto(
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = "Soft Drinks", unitCode = "jc-jug"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("already exists") })
    }

    @Test
    fun `product with an unresolvable unitCode is reported`() {
        val existingCategory = categoryDto("Beverages")
        `when`(productGroupService.getAllGroupDtos()).thenReturn(listOf(groupDto("Soft Drinks", existingCategory.id)))

        val request = BulkProductImportRequestDto(
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = "Soft Drinks", unitCode = "missing-code"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("unitCode 'missing-code'") })
    }

    @Test
    fun `product group without a name is reported`() {
        val request = BulkProductImportRequestDto(
            productGroups = listOf(ProductGroupBulkInsertDto(name = null, categoryName = "Beverages"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Product group at position 1: name is required") })
    }

    @Test
    fun `product group without a categoryName is reported`() {
        val request = BulkProductImportRequestDto(
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = null))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Product group at position 1: categoryName is required") })
    }

    @Test
    fun `duplicate product group name within the payload is reported`() {
        `when`(productCategoryService.getAllCategoryDtos()).thenReturn(listOf(categoryDto("Beverages")))
        val request = BulkProductImportRequestDto(
            productGroups = listOf(
                ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages"),
                ProductGroupBulkInsertDto(name = "soft drinks", categoryName = "Beverages")
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates product group name") })
    }

    @Test
    fun `product group can resolve categoryName against a category in the same payload`() {
        val request = BulkProductImportRequestDto(
            categories = listOf(ProductCategoryBulkInsertDto(name = "Beverages")),
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages"))
        )

        val errors = validator.validate(request)

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun `product without a name is reported`() {
        val request = BulkProductImportRequestDto(
            products = listOf(ProductBulkInsertDto(name = null, groupName = "Soft Drinks", unitCode = "jc-jug"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Product at position 1: name is required") })
    }

    @Test
    fun `product without a groupName is reported`() {
        val request = BulkProductImportRequestDto(
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = null, unitCode = "jc-jug"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Product at position 1: groupName is required") })
    }

    @Test
    fun `product without a unitCode is reported`() {
        val request = BulkProductImportRequestDto(
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = "Soft Drinks", unitCode = null))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Product at position 1: unitCode is required") })
    }

    @Test
    fun `duplicate product name within the payload is reported and the DB check is skipped`() {
        val existingCategory = categoryDto("Beverages")
        `when`(productGroupService.getAllGroupDtos()).thenReturn(listOf(groupDto("Soft Drinks", existingCategory.id)))
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueDto("jc-jug")))

        val request = BulkProductImportRequestDto(
            products = listOf(
                ProductBulkInsertDto(name = "Juice Cola", groupName = "Soft Drinks", unitCode = "jc-jug"),
                ProductBulkInsertDto(name = "juice cola", groupName = "Soft Drinks", unitCode = "jc-jug")
            )
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("duplicates product name") })
        verify(organizationProductService, never()).isDuplicateProductName("juice cola")
    }

    @Test
    fun `product can resolve groupName against a group in the same payload`() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueDto("jc-jug")))
        val request = BulkProductImportRequestDto(
            productGroups = listOf(ProductGroupBulkInsertDto(name = "Soft Drinks", categoryName = "Beverages")),
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = "Soft Drinks", unitCode = "jc-jug"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.none { it.contains("does not match any product group") })
    }

    @Test
    fun `product with an unresolvable groupName is reported`() {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(listOf(unitValueDto("jc-jug")))
        val request = BulkProductImportRequestDto(
            products = listOf(ProductBulkInsertDto(name = "Juice Cola", groupName = "Nonexistent", unitCode = "jc-jug"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("groupName 'Nonexistent'") })
    }

    @Test
    fun `every problem across categories, groups, and products is collected, not just the first`() {
        val request = BulkProductImportRequestDto(
            categories = listOf(ProductCategoryBulkInsertDto(name = null)),
            productGroups = listOf(ProductGroupBulkInsertDto(name = null, categoryName = "Beverages")),
            products = listOf(ProductBulkInsertDto(name = null, groupName = "Soft Drinks", unitCode = "jc-jug"))
        )

        val errors = validator.validate(request)

        assertTrue(errors.any { it.contains("Category at position 1: name is required") })
        assertTrue(errors.any { it.contains("Product group at position 1: name is required") })
        assertTrue(errors.any { it.contains("Product at position 1: name is required") })
        assertEquals(3, errors.size)
    }
}
