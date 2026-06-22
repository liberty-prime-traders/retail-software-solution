package me.ezra_home.retail_software_solution.organizations.business.product_tax_assignment.dto

import java.io.Serializable
import java.time.LocalDate

data class CloseProductTaxAssignmentDto(
    val effectiveTo: LocalDate
) : Serializable
