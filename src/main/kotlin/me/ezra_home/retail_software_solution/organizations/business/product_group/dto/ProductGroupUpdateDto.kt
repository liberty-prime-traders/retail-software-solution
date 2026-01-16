package me.ezra_home.retail_software_solution.organizations.business.product_group.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.ProductGroupEntity}
 */
data class ProductGroupUpdateDto(
  val id: UUID,
  val groupName: Optional<String>? = null,
  val description: Optional<String>? = null
) : Serializable
