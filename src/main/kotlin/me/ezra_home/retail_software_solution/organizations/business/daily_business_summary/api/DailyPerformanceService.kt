package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountCategory
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountTreeNode
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.SummaryRange
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovement
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovementReader
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class DailyPerformanceService(
    private val accountService: AccountService,
    private val ledgerMovementReader: LedgerMovementReader
) {

    fun getPerformance(dailyBusinessSummaryRequest: DailyBusinessSummaryRequest): DailyPerformanceResponse =
        buildPerformance(dailyBusinessSummaryRequest, DateTimes.Offset.Now.organization())

    internal fun buildPerformance(
        dailyBusinessSummaryRequest: DailyBusinessSummaryRequest,
        asOf: OffsetDateTime
    ): DailyPerformanceResponse {
        val range = SummaryRange(dailyBusinessSummaryRequest.from, dailyBusinessSummaryRequest.before, asOf)
        val accountNodesByCode = accountService.getAccountTree().associateBy { accountNode -> accountNode.code }
        val movements = ledgerMovementReader.getMovements(
            from = range.from,
            before = range.before,
            accountCodes = accountNodesByCode.keys,
            sources = PROFIT_AND_LOSS_SOURCES + COLLECTION_SOURCES,
            locationId = SessionContextProvider.getLocationIdOrNull()
        )
        val movementsByDate = movements.groupBy { movement -> movement.localDate }
        val componentsByDate = range.dates.associateWith { date ->
            componentsOf(movementsByDate[date].orEmpty(), accountNodesByCode)
        }
        val totalComponents = componentsByDate.values.reduce(PerformanceComponents::plus)
        return DailyPerformanceResponse(
            asOf = asOf.toInstant(),
            rangeStatus = range.rangeStatus,
            days = componentsByDate.map { (date, components) ->
                DailyPerformanceDay(date, range.statusOf(date), components.toFigures(accountNodesByCode))
            },
            totals = totalComponents.toFigures(accountNodesByCode)
        )
    }

    private fun componentsOf(
        movements: List<LedgerMovement>,
        accountNodesByCode: Map<String, AccountTreeNode>
    ): PerformanceComponents {
        val profitAndLossByAccount = amountsByAccount(movements, PROFIT_AND_LOSS_SOURCES, accountNodesByCode)
        val collectedByAccount = amountsByAccount(movements, PAYMENT_SOURCES, accountNodesByCode)
            .filterKeys { accountCode -> accountCode != SystemAccount.TRADE_RECEIVABLES.code }
        val billedToCustomers = amountsByAccount(movements, BILLING_SOURCES, accountNodesByCode)[SystemAccount.TRADE_RECEIVABLES.code]
        fun profitAndLoss(systemAccount: SystemAccount) = profitAndLossByAccount[systemAccount.code] ?: BigDecimal.ZERO
        return PerformanceComponents(
            grossSales = profitAndLoss(SystemAccount.GROSS_SALES),
            discounts = profitAndLoss(SystemAccount.SALES_DISCOUNTS),
            returnsAndAllowances = profitAndLoss(SystemAccount.SALES_RETURNS) + profitAndLoss(SystemAccount.SALES_ALLOWANCES),
            costOfGoodsSold = profitAndLoss(SystemAccount.COST_OF_GOODS_SOLD) - profitAndLoss(SystemAccount.FREE_GOODS_RECEIVED),
            otherIncome = profitAndLoss(SystemAccount.OTHER_INCOME),
            expenseAmounts = profitAndLossByAccount.filterKeys { accountCode ->
                val node = accountNodesByCode.getValue(accountCode)
                node.category == AccountCategory.EXPENSE && !node.isContra && accountCode != SystemAccount.COST_OF_GOODS_SOLD.code
            },
            collectedAmounts = collectedByAccount,
            billedToCustomers = billedToCustomers ?: BigDecimal.ZERO
        )
    }

    private fun amountsByAccount(
        movements: List<LedgerMovement>,
        sources: Set<LedgerSourceType>,
        accountNodesByCode: Map<String, AccountTreeNode>
    ): Map<String, BigDecimal> =
        movements.filter { movement -> movement.source in sources }
            .groupBy { movement -> movement.accountCode }
            .mapValues { (accountCode, accountMovements) ->
                val accountNode = accountNodesByCode.getValue(accountCode)
                accountMovements.fold(BigDecimal.ZERO) { runningTotal, movement ->
                    runningTotal + accountNode.amountInNormalDirection(movement.entryType, movement.amount)
                }
            }

    private data class PerformanceComponents(
        val grossSales: BigDecimal,
        val discounts: BigDecimal,
        val returnsAndAllowances: BigDecimal,
        val costOfGoodsSold: BigDecimal,
        val otherIncome: BigDecimal,
        val expenseAmounts: Map<String, BigDecimal>,
        val collectedAmounts: Map<String, BigDecimal>,
        val billedToCustomers: BigDecimal
    ) {
        operator fun plus(other: PerformanceComponents) = PerformanceComponents(
            grossSales = grossSales + other.grossSales,
            discounts = discounts + other.discounts,
            returnsAndAllowances = returnsAndAllowances + other.returnsAndAllowances,
            costOfGoodsSold = costOfGoodsSold + other.costOfGoodsSold,
            otherIncome = otherIncome + other.otherIncome,
            expenseAmounts = mergeAmounts(expenseAmounts, other.expenseAmounts),
            collectedAmounts = mergeAmounts(collectedAmounts, other.collectedAmounts),
            billedToCustomers = billedToCustomers + other.billedToCustomers
        )

        fun toFigures(accountNodesByCode: Map<String, AccountTreeNode>): DailyPerformanceFigures {
            val netSales = grossSales - discounts - returnsAndAllowances
            val grossProfit = netSales - costOfGoodsSold
            val expenseLines = toLines(expenseAmounts, accountNodesByCode)
            val expensesTotal = expenseLines.fold(BigDecimal.ZERO) { runningTotal, line -> runningTotal + line.amount }
            val netProfit = grossProfit + otherIncome - expensesTotal
            val collectedLines = toLines(collectedAmounts, accountNodesByCode)
            return DailyPerformanceFigures(
                grossSales = Decimals.Scale4.round(grossSales),
                discounts = Decimals.Scale4.round(discounts),
                returnsAndAllowances = Decimals.Scale4.round(returnsAndAllowances),
                netSales = Decimals.Scale4.round(netSales),
                costOfGoodsSold = Decimals.Scale4.round(costOfGoodsSold),
                grossProfit = Decimals.Scale4.round(grossProfit),
                grossMarginPercent = percentOf(grossProfit, netSales),
                otherIncome = Decimals.Scale4.round(otherIncome),
                expenseLines = expenseLines,
                expensesTotal = Decimals.Scale4.round(expensesTotal),
                netProfit = Decimals.Scale4.round(netProfit),
                netMarginPercent = percentOf(netProfit, netSales),
                collectedByAccount = collectedLines,
                collectedTotal = Decimals.Scale4.round(collectedLines.fold(BigDecimal.ZERO) { runningTotal, line -> runningTotal + line.amount }),
                billedToCustomers = Decimals.Scale4.round(billedToCustomers)
            )
        }

        private fun toLines(amounts: Map<String, BigDecimal>, accountNodesByCode: Map<String, AccountTreeNode>) =
            amounts.entries
                .map { (accountCode, amount) -> SummaryAccountAmount(accountCode, accountNodesByCode.getValue(accountCode).name, Decimals.Scale4.round(amount)) }
                .filter { accountAmount -> accountAmount.amount.signum() != 0 }
                .sortedBy { accountAmount -> accountAmount.accountCode }

        private fun percentOf(amount: BigDecimal, netSales: BigDecimal): BigDecimal? =
            if (Decimals.Scale4.round(netSales).signum() == 0) null
            else amount.multiply(BigDecimal(100)).divide(netSales, 1, RoundingMode.HALF_UP)

        private fun mergeAmounts(first: Map<String, BigDecimal>, second: Map<String, BigDecimal>): Map<String, BigDecimal> =
            (first.keys + second.keys).associateWith { accountCode -> (first[accountCode] ?: BigDecimal.ZERO) + (second[accountCode] ?: BigDecimal.ZERO) }
    }

    private companion object {
        val PROFIT_AND_LOSS_SOURCES = setOf(
            LedgerSourceType.SALE,
            LedgerSourceType.SALE_VOID,
            LedgerSourceType.EXPENSE,
            LedgerSourceType.EXPENSE_VOID,
            LedgerSourceType.STOCK_ADJUSTMENT
        )
        val BILLING_SOURCES = setOf(LedgerSourceType.SALE, LedgerSourceType.SALE_VOID)
        val PAYMENT_SOURCES = setOf(LedgerSourceType.SALE_PAYMENT, LedgerSourceType.SALE_PAYMENT_VOID)
        val COLLECTION_SOURCES = BILLING_SOURCES + PAYMENT_SOURCES
    }
}
