package me.ezra_home.retail_software_solution.locations.business.sync.sync_services.product

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.util.enums.ProductStatusConverter
import java.util.UUID

object ProductSyncDataMapper {
  private val statusConverter = ProductStatusConverter()

  fun fromTuple(tuple: Tuple): ProductSyncData {
    val statusString = tuple.getRequired(ProductQueryConstants.Columns.STATUS, String::class.java)
    val status = statusConverter.convertToEntityAttribute(statusString)
      ?: throw IllegalStateException("Invalid status value: $statusString")

    return ProductSyncData(
      productId = tuple.getRequired(ProductQueryConstants.Columns.ID, UUID::class.java),
      productName = tuple.getRequired(ProductQueryConstants.Columns.NAME, String::class.java),
      description = tuple.getOptional(ProductQueryConstants.Columns.DESCRIPTION, String::class.java),
      productGroupName = tuple.getOptional(ProductQueryConstants.Columns.PRODUCT_GROUP_NAME, String::class.java),
      status = status,
      referenceNumber = tuple.getOptional(ProductQueryConstants.Columns.REFERENCE_NUMBER, String::class.java),
      baseUnitId = tuple.getRequired(ProductQueryConstants.Columns.BASE_UNIT_ID, UUID::class.java),
      categoryId = tuple.getRequired(ProductQueryConstants.Columns.CATEGORY_ID, UUID::class.java),
      revision = tuple.getOptional(ProductQueryConstants.Columns.REVISION, Number::class.java)?.toLong()
    )
  }

  private fun <T> Tuple.getRequired(alias: String, type: Class<T>): T {
    return get(alias, type)
      ?: throw IllegalStateException("Required column '$alias' is null")
  }

  private fun <T> Tuple.getOptional(alias: String, type: Class<T>): T? {
    return get(alias, type)
  }
}
