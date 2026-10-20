package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class DailyPerformanceResponse(
    val asOf: Instant,
    val rangeStatus: PeriodStatus,
    val days: List<DailyPerformanceDay>,
    val totals: DailyPerformanceFigures
)

data class DailyPerformanceDay(
    val date: LocalDate,
    val status: PeriodStatus,
    val figures: DailyPerformanceFigures
)

data class DailyPerformanceFigures(
    val grossSales: BigDecimal,
    val discounts: BigDecimal,
    val returnsAndAllowances: BigDecimal,
    val netSales: BigDecimal,
    val costOfGoodsSold: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercent: BigDecimal?,
    val otherIncome: BigDecimal,
    val expenseLines: List<SummaryAccountAmount>,
    val expensesTotal: BigDecimal,
    val netProfit: BigDecimal,
    val netMarginPercent: BigDecimal?,
    val collectedByAccount: List<SummaryAccountAmount>,
    val collectedTotal: BigDecimal,
    val billedToCustomers: BigDecimal
)

data class SummaryAccountAmount(
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal
)
