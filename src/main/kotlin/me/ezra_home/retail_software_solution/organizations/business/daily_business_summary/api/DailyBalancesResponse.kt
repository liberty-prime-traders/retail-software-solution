package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api

import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
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
    val netWorthEffect: BalanceEffect,
    val netWorthChange: BigDecimal,
    val netWorthChangeEffect: BalanceEffect
)

data class BalanceSection(
    val lines: List<AccountBalanceLine>,
    val closingTotal: BigDecimal,
    val changeTotal: BigDecimal,
    val changeTotalEffect: BalanceEffect
)

data class AccountBalanceLine(
    val accountCode: String?,
    val accountName: String,
    val closingBalance: BigDecimal,
    val closingBalanceEffect: BalanceEffect,
    val change: BigDecimal,
    val changeEffect: BalanceEffect
)
