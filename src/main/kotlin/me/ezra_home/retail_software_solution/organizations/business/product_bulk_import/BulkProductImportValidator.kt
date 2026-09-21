package me.ezra_home.retail_software_solution.organizations.business.product_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.BulkProductImportRequestDto
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.ProductBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.ProductCategoryBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api.ProductGroupBulkInsertDto
import me.ezra_home.retail_software_solution.organizations.business.product_category.api.ProductCategoryService
import me.ezra_home.retail_software_solution.organizations.business.product_group.api.ProductGroupService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Component

@Component
class BulkProductImportValidator(
    private val productCategoryService: ProductCategoryService,
    private val productGroupService: ProductGroupService,
    private val organizationProductService: OrganizationProductService,
    private val unitValueFetcher: UnitValueFetcher
) {

    fun validate(bulkProductImportRequestDto: BulkProductImportRequestDto): List<String> {
        val errors = mutableListOf<String>()
        validateCategories(bulkProductImportRequestDto.categories, errors)
        validateProductGroups(bulkProductImportRequestDto.productGroups, bulkProductImportRequestDto.categories, errors)
        validateProducts(bulkProductImportRequestDto.products, bulkProductImportRequestDto.productGroups, errors)
        return errors
    }

    private fun validateCategories(categories: List<ProductCategoryBulkInsertDto>, errors: MutableList<String>) {
        categories.forEachIndexed { index, category ->
            if (!StringUtils.hasValue(category.name)) {
                errors.add("Category at position ${index + 1}: name is required")
                return@forEachIndexed
            }
            val duplicateIndex = categories.take(index).indexOfFirst { StringUtils.isEquivalent(it.name, category.name) }
            if (duplicateIndex != -1) {
                errors.add("Category at position ${index + 1}: duplicates category name '${category.name}' at position ${duplicateIndex + 1}")
            }
        }
    }

    private fun validateProductGroups(
        productGroups: List<ProductGroupBulkInsertDto>,
        payloadCategories: List<ProductCategoryBulkInsertDto>,
        errors: MutableList<String>
    ) {
        val existingCategories = productCategoryService.getAllCategoryDtos()
        productGroups.forEachIndexed { index, productGroup ->
            if (!StringUtils.hasValue(productGroup.name)) {
                errors.add("Product group at position ${index + 1}: name is required")
                return@forEachIndexed
            }
            if (!StringUtils.hasValue(productGroup.categoryName)) {
                errors.add("Product group at position ${index + 1}: categoryName is required")
                return@forEachIndexed
            }
            val duplicateIndex = productGroups.take(index).indexOfFirst { StringUtils.isEquivalent(it.name, productGroup.name) }
            if (duplicateIndex != -1) {
                errors.add("Product group at position ${index + 1}: duplicates product group name '${productGroup.name}' at position ${duplicateIndex + 1}")
            }
            val categoryResolvable = payloadCategories.any { StringUtils.isEquivalent(it.name, productGroup.categoryName) } ||
                existingCategories.any { StringUtils.isEquivalent(it.categoryName, productGroup.categoryName) }
            if (!categoryResolvable) {
                errors.add("Product group at position ${index + 1}: categoryName '${productGroup.categoryName}' does not match any category in this payload or in the organization")
            }
        }
    }

    private fun validateProducts(
        products: List<ProductBulkInsertDto>,
        payloadProductGroups: List<ProductGroupBulkInsertDto>,
        errors: MutableList<String>
    ) {
        val existingUnitValues = unitValueFetcher.getAllUnitValues()
        val existingGroups = productGroupService.getAllGroupDtos()

        products.forEachIndexed { index, product ->
            if (!StringUtils.hasValue(product.name)) {
                errors.add("Product at position ${index + 1}: name is required")
                return@forEachIndexed
            }
            if (!StringUtils.hasValue(product.groupName)) {
                errors.add("Product at position ${index + 1}: groupName is required")
                return@forEachIndexed
            }
            if (!StringUtils.hasValue(product.unitCode)) {
                errors.add("Product at position ${index + 1}: unitCode is required")
                return@forEachIndexed
            }

            val duplicateIndex = products.take(index).indexOfFirst { StringUtils.isEquivalent(it.name, product.name) }
            if (duplicateIndex != -1) {
                errors.add("Product at position ${index + 1}: duplicates product name '${product.name}' at position ${duplicateIndex + 1}")
            } else if (organizationProductService.isDuplicateProductName(product.name!!)) {
                errors.add("Product at position ${index + 1}: a product with the name '${product.name}' already exists")
            }

            val groupResolvable = payloadProductGroups.any { StringUtils.isEquivalent(it.name, product.groupName) } ||
                existingGroups.any { StringUtils.isEquivalent(it.groupName, product.groupName) }
            if (!groupResolvable) {
                errors.add("Product at position ${index + 1}: groupName '${product.groupName}' does not match any product group in this payload or in the organization")
            }

            val unitResolvable = existingUnitValues.any { StringUtils.isEquivalent(it.code, product.unitCode) }
            if (!unitResolvable) {
                errors.add("Product at position ${index + 1}: unitCode '${product.unitCode}' does not match any existing unit value")
            }
        }
    }
}
