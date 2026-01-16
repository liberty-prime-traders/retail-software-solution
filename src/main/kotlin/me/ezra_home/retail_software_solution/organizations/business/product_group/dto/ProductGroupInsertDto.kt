package me.ezra_home.retail_software_solution.organizations.business.product_group.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.ProductGroupEntity}
 */
data class ProductGroupInsertDto(
  val groupName: String,
  val description: String? = null
) : Serializable
