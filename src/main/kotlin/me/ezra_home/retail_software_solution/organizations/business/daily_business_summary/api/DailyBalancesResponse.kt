package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class DailyBalancesResponse(
    val asOf: Instant,
    val rangeStatus: PeriodStatus,
    val days: List<DailyBalancesDay>,
    val rangeBalances: BalanceSections
)

data class DailyBalancesDay(
    val date: LocalDate,
    val status: PeriodStatus,
    val balances: BalanceSections
)

data class BalanceSections(
    val assets: BalanceSection,
    val liabilities: BalanceSection,
    val netWorth: BigDecimal,
    val netWorthChange: BigDecimal
)

data class BalanceSection(
    val lines: List<AccountBalanceLine>,
    val closingTotal: BigDecimal,
    val changeTotal: BigDecimal
)

data class AccountBalanceLine(
    val accountCode: String?,
    val accountName: String,
    val closingBalance: BigDecimal,
    val change: BigDecimal
)
