package me.ezra_home.retail_software_solution.organizations.business.product.dto

import java.io.Serializable
import java.util.UUID

data class TagSummaryDto(
    val id: UUID,
    val tagName: String?
) : Serializable
