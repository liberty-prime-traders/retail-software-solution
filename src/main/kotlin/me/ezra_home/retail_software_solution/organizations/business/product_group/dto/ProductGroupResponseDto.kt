package me.ezra_home.retail_software_solution.organizations.business.product_group.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.ProductGroupEntity}
 */
data class ProductGroupResponseDto(
  val id: UUID?,
  val groupName: String?,
  val description: String?,
  val categoryId: UUID?,
  val categoryName: String?,
  val createdBy: String?,
  val createdOn: OffsetDateTime?,
  val referenceNumber: String?
) : Serializable
