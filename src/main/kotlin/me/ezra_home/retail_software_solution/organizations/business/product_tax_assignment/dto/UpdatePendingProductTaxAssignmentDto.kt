package me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto

import java.io.Serializable
import java.time.LocalDate

data class UpdatePendingProductTaxAssignmentDto(
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null
) : Serializable
