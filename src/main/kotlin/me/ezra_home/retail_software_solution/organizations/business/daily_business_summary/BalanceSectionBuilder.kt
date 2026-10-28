package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountCategory
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountTreeNode
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.AccountBalanceLine
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.BalanceSection
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.BalanceSections
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import java.math.BigDecimal

internal class BalanceSectionBuilder(accountNodes: List<AccountTreeNode>, private val trackedLeaves: List<AccountTreeNode>) {
    private val nodesByCode = accountNodes.associateBy { accountNode -> accountNode.code }

    private val digitalPaymentsLeafCodes = leafCodesUnder(SystemAccount.DIGITAL_PAYMENTS)
    private val taxPayableLeafCodes = leafCodesUnder(SystemAccount.TAX_PAYABLE)
    private val fixedLiabilityCodes = listOf(SystemAccount.TRADE_PAYABLES, SystemAccount.WAGES_PAYABLE, SystemAccount.LOANS_PAYABLE)
        .map { systemAccount -> systemAccount.code }

    fun build(closing: Map<String, BigDecimal>, change: Map<String, BigDecimal>): BalanceSections {
        val assets = buildSection(
            category = AccountCategory.ASSET,
            leadingFixedCodes = listOf(SystemAccount.CASH.code),
            variableCodes = digitalPaymentsLeafCodes,
            trailingFixedCodes = listOf(SystemAccount.TRADE_RECEIVABLES.code, SystemAccount.INVENTORY.code),
            otherName = OTHER_ASSETS,
            closing = closing,
            changeByAccountCode = change
        )
        val liabilities = buildSection(
            category = AccountCategory.LIABILITY,
            leadingFixedCodes = fixedLiabilityCodes,
            variableCodes = taxPayableLeafCodes,
            trailingFixedCodes = emptyList(),
            otherName = OTHER_LIABILITIES,
            closing = closing,
            changeByAccountCode = change
        )
        val netWorth = Decimals.MoneyScale.round(assets.closingTotal - liabilities.closingTotal)
        val netWorthChange = Decimals.MoneyScale.round(assets.changeTotal - liabilities.changeTotal)
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
        changeByAccountCode: Map<String, BigDecimal>
    ): BalanceSection {
        val sectionLeaves = trackedLeaves.filter { trackedLeaf -> trackedLeaf.category == category }
        val listedCodes = (leadingFixedCodes + variableCodes + trailingFixedCodes).toSet()
        fun signedAmount(amounts: Map<String, BigDecimal>, node: AccountTreeNode): BigDecimal {
            val amount = amounts[node.code] ?: BigDecimal.ZERO
            return if (node.isContra) amount.negate() else amount
        }
        val effectWhenIncreasing = category.balanceEffectWhenIncreasing()
        fun lineOf(accountCode: String?, accountName: String, closingBalance: BigDecimal, accountChange: BigDecimal): AccountBalanceLine {
            val roundedClosingBalance = Decimals.MoneyScale.round(closingBalance)
            val roundedChange = Decimals.MoneyScale.round(accountChange)
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
                accountChange = node?.let { accountNode -> signedAmount(changeByAccountCode, accountNode) } ?: BigDecimal.ZERO
            )
        }
        fun isActive(line: AccountBalanceLine) = line.closingBalance.signum() != 0 || line.change.signum() != 0

        val otherLeaves = sectionLeaves.filter { sectionLeaf -> sectionLeaf.code !in listedCodes }
        val otherLine = lineOf(
            accountCode = null,
            accountName = otherName,
            closingBalance = otherLeaves.fold(BigDecimal.ZERO) { total, node -> total + signedAmount(closing, node) },
            accountChange = otherLeaves.fold(BigDecimal.ZERO) { total, node -> total + signedAmount(changeByAccountCode, node) }
        )
        val lines = leadingFixedCodes.map(::lineFor) +
            variableCodes.map(::lineFor).filter(::isActive) +
            trailingFixedCodes.map(::lineFor) +
            listOf(otherLine).filter(::isActive)
        val changeTotal = Decimals.MoneyScale.round(lines.fold(BigDecimal.ZERO) { total, line -> total + line.change })
        return BalanceSection(
            lines = lines,
            closingTotal = Decimals.MoneyScale.round(lines.fold(BigDecimal.ZERO) { total, line -> total + line.closingBalance }),
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

    private companion object {
        const val OTHER_ASSETS = "Other assets"
        const val OTHER_LIABILITIES = "Other liabilities"
    }
}
