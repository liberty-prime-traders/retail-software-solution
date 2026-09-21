package me.ezra_home.retail_software_solution.organizations.business.product_bulk_import.api

data class BulkProductImportRequestDto(
    val categories: List<ProductCategoryBulkInsertDto> = emptyList(),
    val productGroups: List<ProductGroupBulkInsertDto> = emptyList(),
    val products: List<ProductBulkInsertDto> = emptyList()
)

data class ProductCategoryBulkInsertDto(
    val name: String?,
    val description: String? = null
)

data class ProductGroupBulkInsertDto(
    val name: String?,
    val categoryName: String?,
    val description: String? = null
)

data class ProductBulkInsertDto(
    val name: String?,
    val description: String? = null,
    val groupName: String?,
    val unitCode: String?
)
