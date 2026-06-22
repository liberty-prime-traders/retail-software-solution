package me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto

import java.io.Serializable
import java.time.LocalDate
import java.util.UUID

data class ProductTaxAssignmentInsertDto(
    val productId: UUID,
    val taxRateId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
) : Serializable
