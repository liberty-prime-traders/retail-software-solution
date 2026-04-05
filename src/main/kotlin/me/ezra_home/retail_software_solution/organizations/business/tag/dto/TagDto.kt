package me.ezra_home.retail_software_solution.organizations.business.tag.dto

import me.ezra_home.retail_software_solution.organizations.business.tag.public.CategoryType
import java.time.OffsetDateTime
import java.util.UUID

data class TagDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var category: CategoryType,
    var tagName: String,
    var description: String? = null
)
