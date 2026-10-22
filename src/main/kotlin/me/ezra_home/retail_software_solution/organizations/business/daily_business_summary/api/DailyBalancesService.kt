package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountCategory
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountTreeNode
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.SummaryRange
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerAccountMovement
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovementReader
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class DailyBalancesService(
    private val accountService: AccountService,
    private val ledgerMovementReader: LedgerMovementReader
) {

    // REQUIRES_NEW because a joined transaction keeps its own isolation level and would silently drop REPEATABLE_READ.
    @TransactionalOnOrganizationSchema(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    fun getBalances(dailyBusinessSummaryRequest: DailyBusinessSummaryRequest): DailyBalancesResponse =
        buildBalances(dailyBusinessSummaryRequest, DateTimes.Offset.Now.organization())

    internal fun buildBalances(
        dailyBusinessSummaryRequest: DailyBusinessSummaryRequest,
        asOf: OffsetDateTime
    ): DailyBalancesResponse {
        val range = SummaryRange(dailyBusinessSummaryRequest.from, dailyBusinessSummaryRequest.before, asOf)
        val accountNodes = accountService.getAccountTree()
        val trackedLeaves = accountNodes.filter { accountNode -> accountNode.isLeaf && accountNode.category in TRACKED_CATEGORIES }
        val trackedCodes = trackedLeaves.map { trackedLeaf -> trackedLeaf.code }.toSet()
        val currentBalances = accountService.getFreshBalancesByCodes(trackedCodes)
        val movements = ledgerMovementReader.getAccountMovementsFrom(range.from, range.before, trackedCodes)
        val netMovementsByAccount = netMovementsByAccount(movements, trackedLeaves.associateBy { trackedLeaf -> trackedLeaf.code })

        val closingByDate = LinkedHashMap<LocalDate, Map<String, BigDecimal>>()
        var closingAfterDate = trackedCodes.associateWith { accountCode ->
            val afterRangeMovement = netMovementsByAccount[accountCode].orEmpty()
                .filterKeys { movementDate -> !movementDate.isBefore(range.before) }.values.fold(BigDecimal.ZERO, BigDecimal::add)
            (currentBalances[accountCode] ?: BigDecimal.ZERO) - afterRangeMovement
        }
        range.dates.asReversed().forEach { date ->
            closingByDate[date] = closingAfterDate
            closingAfterDate = closingAfterDate.mapValues { (accountCode, closing) ->
                closing - (netMovementsByAccount[accountCode]?.get(date) ?: BigDecimal.ZERO)
            }
        }
        val closingBeforeRange = closingAfterDate
        val sectionBuilder = SectionBuilder(accountNodes, trackedLeaves)

        val days = range.dates.map { date ->
            val closing = closingByDate.getValue(date)
            val change = trackedCodes.associateWith { accountCode -> netMovementsByAccount[accountCode]?.get(date) ?: BigDecimal.ZERO }
            DailyBalancesDay(date, range.statusOf(date), sectionBuilder.build(closing, change))
        }
        val lastClosing = closingByDate.getValue(range.dates.last())
        val rangeChange = trackedCodes.associateWith { accountCode -> lastClosing.getValue(accountCode) - closingBeforeRange.getValue(accountCode) }
        return DailyBalancesResponse(
            asOf = asOf.toInstant(),
            rangeStatus = range.rangeStatus,
            days = days,
            rangeBalances = sectionBuilder.build(lastClosing, rangeChange)
        )
    }

    private fun netMovementsByAccount(
        movements: List<LedgerAccountMovement>,
        trackedLeavesByCode: Map<String, AccountTreeNode>
    ): Map<String, Map<LocalDate, BigDecimal>> =
        movements.groupBy { movement -> movement.accountCode }.mapValues { (accountCode, accountMovements) ->
            val accountNode = trackedLeavesByCode.getValue(accountCode)
            accountMovements.groupBy { movement -> movement.localDate }.mapValues { (_, dayMovements) ->
                dayMovements.fold(BigDecimal.ZERO) { runningTotal, movement ->
                    runningTotal + accountNode.amountInNormalDirection(movement.entryType, movement.amount)
                }
            }
        }

    private class SectionBuilder(accountNodes: List<AccountTreeNode>, private val trackedLeaves: List<AccountTreeNode>) {
        private val nodesByCode = accountNodes.associateBy { accountNode -> accountNode.code }

        private val digitalPaymentsLeafCodes = leafCodesUnder(SystemAccount.DIGITAL_PAYMENTS)
        private val taxPayableLeafCodes = leafCodesUnder(SystemAccount.TAX_PAYABLE)
        private val fixedLiabilityCodes = listOf(SystemAccount.TRADE_PAYABLES, SystemAccount.WAGES_PAYABLE).map { systemAccount -> systemAccount.code }

        fun build(closing: Map<String, BigDecimal>, change: Map<String, BigDecimal>): BalanceSections {
            val assets = buildSection(
                category = AccountCategory.ASSET,
                leadingFixedCodes = listOf(SystemAccount.CASH.code),
                variableCodes = digitalPaymentsLeafCodes,
                trailingFixedCodes = listOf(SystemAccount.TRADE_RECEIVABLES.code, SystemAccount.INVENTORY.code),
                otherName = OTHER_ASSETS,
                closing = closing,
                change = change
            )
            val liabilities = buildSection(
                category = AccountCategory.LIABILITY,
                leadingFixedCodes = fixedLiabilityCodes,
                variableCodes = taxPayableLeafCodes,
                trailingFixedCodes = emptyList(),
                otherName = OTHER_LIABILITIES,
                closing = closing,
                change = change
            )
            val netWorth = Decimals.Scale2.round(assets.closingTotal - liabilities.closingTotal)
            val netWorthChange = Decimals.Scale2.round(assets.changeTotal - liabilities.changeTotal)
            return BalanceSections(
                assets = assets,
                liabilities = liabilities,
                netWorth = netWorth,
                netWorthEffect = BalanceEffect.ofSignedAmount(netWorth),
                netWorthChange = netWorthChange,
                netWorthChangeEffect = BalanceEffect.ofSignedAmount(netWorthChange)
            )
        }

        private fun buildSection(
            category: AccountCategory,
            leadingFixedCodes: List<String>,
            variableCodes: List<String>,
            trailingFixedCodes: List<String>,
            otherName: String,
            closing: Map<String, BigDecimal>,
            change: Map<String, BigDecimal>
        ): BalanceSection {
            val sectionLeaves = trackedLeaves.filter { trackedLeaf -> trackedLeaf.category == category }
            val listedCodes = (leadingFixedCodes + variableCodes + trailingFixedCodes).toSet()
            fun signedAmount(amounts: Map<String, BigDecimal>, node: AccountTreeNode): BigDecimal {
                val amount = amounts[node.code] ?: BigDecimal.ZERO
                return if (node.isContra) amount.negate() else amount
            }
            val effectWhenIncreasing = category.balanceEffectWhenIncreasing()
            fun lineOf(accountCode: String?, accountName: String, closingBalance: BigDecimal, change: BigDecimal): AccountBalanceLine {
                val roundedClosingBalance = Decimals.Scale2.round(closingBalance)
                val roundedChange = Decimals.Scale2.round(change)
                return AccountBalanceLine(
                    accountCode = accountCode,
                    accountName = accountName,
                    closingBalance = roundedClosingBalance,
                    closingBalanceEffect = BalanceEffect.ofBalance(roundedClosingBalance, effectWhenIncreasing),
                    change = roundedChange,
                    changeEffect = BalanceEffect.ofChange(roundedChange, effectWhenIncreasing)
                )
            }
            fun lineFor(accountCode: String): AccountBalanceLine {
                val node = nodesByCode[accountCode]
                return lineOf(
                    accountCode = accountCode,
                    accountName = node?.name ?: SystemAccount.fromCode(accountCode)?.accountName ?: accountCode,
                    closingBalance = node?.let { accountNode -> signedAmount(closing, accountNode) } ?: BigDecimal.ZERO,
                    change = node?.let { accountNode -> signedAmount(change, accountNode) } ?: BigDecimal.ZERO
                )
            }
            fun isActive(line: AccountBalanceLine) = line.closingBalance.signum() != 0 || line.change.signum() != 0

            val otherLeaves = sectionLeaves.filter { sectionLeaf -> sectionLeaf.code !in listedCodes }
            val otherLine = lineOf(
                accountCode = null,
                accountName = otherName,
                closingBalance = otherLeaves.fold(BigDecimal.ZERO) { total, node -> total + signedAmount(closing, node) },
                change = otherLeaves.fold(BigDecimal.ZERO) { total, node -> total + signedAmount(change, node) }
            )
            val lines = leadingFixedCodes.map(::lineFor) +
                variableCodes.map(::lineFor).filter(::isActive) +
                trailingFixedCodes.map(::lineFor) +
                listOf(otherLine).filter(::isActive)
            val changeTotal = Decimals.Scale2.round(lines.fold(BigDecimal.ZERO) { total, line -> total + line.change })
            return BalanceSection(
                lines = lines,
                closingTotal = Decimals.Scale2.round(lines.fold(BigDecimal.ZERO) { total, line -> total + line.closingBalance }),
                changeTotal = changeTotal,
                changeTotalEffect = BalanceEffect.ofChange(changeTotal, effectWhenIncreasing)
            )
        }

        // A childless extension point is itself the leaf that receives postings.
        private fun leafCodesUnder(parent: SystemAccount): List<String> {
            val parentIsLeaf = nodesByCode[parent.code]?.isLeaf == true
            if (parentIsLeaf) return listOf(parent.code)
            return trackedLeaves.filter { trackedLeaf -> trackedLeaf.parentAccountCode == parent.code }.map { trackedLeaf -> trackedLeaf.code }.sorted()
        }
    }

    private companion object {
        val TRACKED_CATEGORIES = setOf(AccountCategory.ASSET, AccountCategory.LIABILITY)
        const val OTHER_ASSETS = "Other assets"
        const val OTHER_LIABILITIES = "Other liabilities"
    }
}
