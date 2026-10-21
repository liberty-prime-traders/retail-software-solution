package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api

import java.time.LocalDate

data class DailyBusinessSummaryRequest(
    val from: LocalDate,
    val before: LocalDate
)
