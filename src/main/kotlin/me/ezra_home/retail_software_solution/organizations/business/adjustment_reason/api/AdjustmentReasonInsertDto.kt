package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api

data class AdjustmentReasonInsertDto(
    val name: String,
    val direction: AdjustmentDirection
)
