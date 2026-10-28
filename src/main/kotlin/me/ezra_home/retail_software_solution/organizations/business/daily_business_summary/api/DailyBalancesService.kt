package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountCategory
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountTreeNode
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.BalanceSectionBuilder
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.SummaryRange
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerAccountMovement
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovementReader
import me.ezra_home.retail_software_solution.util.business.DateTimes
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
        val sectionBuilder = BalanceSectionBuilder(accountNodes, trackedLeaves)

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

    private companion object {
        val TRACKED_CATEGORIES = setOf(AccountCategory.ASSET, AccountCategory.LIABILITY)
    }
}
