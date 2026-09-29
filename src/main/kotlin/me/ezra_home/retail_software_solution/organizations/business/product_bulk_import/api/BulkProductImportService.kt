package me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.BulkProductImportValidator
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryService
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class BulkProductImportService(
    private val bulkProductImportValidator: BulkProductImportValidator,
    private val productCategoryService: ProductCategoryService,
    private val productGroupService: ProductGroupService,
    private val organizationProductService: OrganizationProductService,
    private val unitValueFetcher: UnitValueFetcher
) {

    fun bulkImport(bulkProductImportRequestDto: BulkProductImportRequestDto) {
        val errors = bulkProductImportValidator.validate(bulkProductImportRequestDto)
        if (errors.isNotEmpty()) {
            throw RtsGenericException("Bulk product import validation failed", errors)
        }
        val categoryIdByNormalizedName = saveOrReuseCategories(bulkProductImportRequestDto.categories)
        val groupIdByNormalizedName = saveOrReuseProductGroups(bulkProductImportRequestDto.productGroups, categoryIdByNormalizedName)
        saveProducts(bulkProductImportRequestDto.products, groupIdByNormalizedName)
    }

    private fun saveOrReuseCategories(categories: List<ProductCategoryBulkInsertDto>): Map<String, UUID> {
        val categoryIdByNormalizedName = productCategoryService.getAllCategoryDtos()
            .associateTo(mutableMapOf()) { StringUtils.normalizeForComparison(it.categoryName!!) to it.id }

        val newCategories = categories.filter {
            !categoryIdByNormalizedName.containsKey(StringUtils.normalizeForComparison(it.name!!))
        }
        val saved = productCategoryService.bulkCreateValidatedList(
            newCategories.map { ProductCategoryInsertDto(categoryName = it.name, description = it.description) }
        )
        saved.forEach { categoryIdByNormalizedName[StringUtils.normalizeForComparison(it.categoryName!!)] = it.id }

        return categoryIdByNormalizedName
    }

    private fun saveOrReuseProductGroups(
        productGroups: List<ProductGroupBulkInsertDto>,
        categoryIdByNormalizedName: Map<String, UUID>
    ): Map<String, UUID> {
        val groupIdByNormalizedName = productGroupService.getAllGroupDtos()
            .associateTo(mutableMapOf()) { StringUtils.normalizeForComparison(it.groupName) to it.id }

        val newProductGroups = productGroups.filter {
            !groupIdByNormalizedName.containsKey(StringUtils.normalizeForComparison(it.name!!))
        }
        val saved = productGroupService.bulkCreateValidatedList(
            newProductGroups.map {
                ProductGroupInsertDto(
                    groupName = it.name!!,
                    description = it.description,
                    categoryId = categoryIdByNormalizedName.getValue(StringUtils.normalizeForComparison(it.categoryName!!))
                )
            }
        )
        saved.forEach { groupIdByNormalizedName[StringUtils.normalizeForComparison(it.groupName!!)] = it.id }

        return groupIdByNormalizedName
    }

    private fun saveProducts(products: List<ProductBulkInsertDto>, groupIdByNormalizedName: Map<String, UUID>) {
        val unitIdByNormalizedCode = unitValueFetcher.getAllUnitValues()
            .associate { StringUtils.normalizeForComparison(it.code) to it.id }

        val insertDtos = products.map { product ->
            OrganizationProductInsertDto(
                productName = product.name!!,
                description = product.description,
                productGroupId = groupIdByNormalizedName.getValue(StringUtils.normalizeForComparison(product.groupName!!)),
                baseUnitId = unitIdByNormalizedCode.getValue(StringUtils.normalizeForComparison(product.unitCode!!))
            )
        }
        organizationProductService.bulkCreateValidatedList(insertDtos)
    }
}
