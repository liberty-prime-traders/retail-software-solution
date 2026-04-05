package me.ezra_home.retail_software_solution.organizations.business.product_category.mapping

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product_category.ProductCategoryCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class ProductCategoryNameQualifier(private val productCategoryCache: ProductCategoryCache,) {

    @ProductCategoryName
    fun resolveCategoryName(categoryId: UUID?): String? {
        return categoryId?.let { productCategoryCache.getCategoriesById()[it]?.categoryName }
    }
}
