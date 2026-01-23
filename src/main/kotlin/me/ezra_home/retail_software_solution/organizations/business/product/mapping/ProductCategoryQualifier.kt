package me.ezra_home.retail_software_solution.organizations.business.product.mapping

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.product_category.ProductCategoryCache
import me.ezra_home.retail_software_solution.organizations.business.product_group.ProductGroupCache
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProductCategoryName

@Qualifier
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProductCategoryId

@Component
@TransactionalOnOrganizationSchema
class ProductCategoryQualifier(
  private val productGroupCache: ProductGroupCache,
  private val productCategoryCache: ProductCategoryCache
) {

  @ProductCategoryName
  fun resolveCategoryName(productGroupId: UUID?): String? {
    val productGroup = productGroupId?.let { pgId ->
      productGroupCache.findAllProductGroups().find { it.id == pgId }
    }
    val categoryId = productGroup?.categoryId
    val categoriesById = productCategoryCache.getCategoriesById()
    return categoryId?.let { categoriesById[it]?.categoryName }
  }

  @ProductCategoryId
  fun resolveCategoryId(productGroupId: UUID?): UUID? {
    val productGroup = productGroupId?.let { pgId ->
      productGroupCache.findAllProductGroups().find { it.id == pgId }
    }
    return productGroup?.categoryId
  }
}
