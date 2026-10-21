package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountTreeNode
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.BalanceSection
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyBalancesResponse
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyBalancesService
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyBusinessSummaryRequest
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.PeriodStatus
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerAccountMovement
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovementReader
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class DailyBalancesServiceTest {

    private val digitalWalletCode = "001.001.005.001"
    private val asOf = OffsetDateTime.of(2026, 3, 10, 12, 0, 0, 0, ZoneOffset.UTC)
    private val day8 = LocalDate.of(2026, 3, 8)
    private val day9 = LocalDate.of(2026, 3, 9)
    private val day10 = LocalDate.of(2026, 3, 10)

    private var currentBalances: Map<String, BigDecimal> = emptyMap()

    @BeforeEach
    fun setOrganizationSession() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(UUID.randomUUID(), "org-a", "UTC")))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `closing balances walk back from the current balance and a later posting lowers earlier closings by its amount`() {
        currentBalances = mapOf(SystemAccount.CASH.code to BigDecimal(1000))

        val dailyBalancesResponse = build(
            DailyBusinessSummaryRequest(day8, day10),
            movement(day9, SystemAccount.CASH, EntryType.DEBIT, 100),
            movement(day10, SystemAccount.CASH, EntryType.DEBIT, 50)
        )

        assertEquals(listOf(850, 950), dailyBalancesResponse.days.map { day -> cashClosing(day.balances.assets) })
        assertEquals(listOf(0, 100), dailyBalancesResponse.days.map { day -> cashChange(day.balances.assets) })
    }

    @Test
    fun `the day containing asOf closes at the current balance`() {
        currentBalances = mapOf(SystemAccount.CASH.code to BigDecimal(1000))

        val dailyBalancesResponse = build(
            DailyBusinessSummaryRequest(day9, LocalDate.of(2026, 3, 11)),
            movement(day10, SystemAccount.CASH, EntryType.DEBIT, 70)
        )

        val today = dailyBalancesResponse.days.last()
        assertEquals(PeriodStatus.IN_PROGRESS, today.status)
        assertEquals(1000, cashClosing(today.balances.assets))
        assertEquals(930, cashClosing(dailyBalancesResponse.days.first().balances.assets))
        assertEquals(PeriodStatus.IN_PROGRESS, dailyBalancesResponse.rangeStatus)
    }

    @Test
    fun `balances and movements are read in one repeatable read snapshot of their own transaction`() {
        val transactionSettings = DailyBalancesService::class.java.getMethod("getBalances", DailyBusinessSummaryRequest::class.java)
            .getAnnotation(TransactionalOnOrganizationSchema::class.java)

        assertNotNull(transactionSettings)
        assertEquals(Isolation.REPEATABLE_READ, transactionSettings.isolation)
        assertEquals(Propagation.REQUIRES_NEW, transactionSettings.propagation)
    }

    @Test
    fun `a payment account under digital payments is listed as its own asset line`() {
        currentBalances = mapOf(digitalWalletCode to BigDecimal(200))

        val dailyBalancesResponse = build(
            DailyBusinessSummaryRequest(day9, day10),
            LedgerAccountMovement(day9, digitalWalletCode, EntryType.DEBIT, BigDecimal(200)),
            tree = RecordingAccountService.accountTree(digitalWalletCode)
        )

        val walletLine = dailyBalancesResponse.days.single().balances.assets.lines.single { line -> line.accountCode == digitalWalletCode }
        assertEquals(0, BigDecimal(200).compareTo(walletLine.closingBalance))
        assertEquals(0, BigDecimal(200).compareTo(walletLine.change))
    }

    @Test
    fun `fixed assets net of accumulated depreciation roll into other assets and section totals equal their lines`() {
        currentBalances = mapOf(
            SystemAccount.CASH.code to BigDecimal(100),
            SystemAccount.EQUIPMENT.code to BigDecimal(500),
            SystemAccount.ACCUMULATED_DEPRECIATION.code to BigDecimal(100)
        )

        val assets = build(DailyBusinessSummaryRequest(day9, day10)).days.single().balances.assets

        val otherAssets = assets.lines.single { line -> line.accountCode == null }
        assertEquals("Other assets", otherAssets.accountName)
        assertEquals(0, BigDecimal(400).compareTo(otherAssets.closingBalance))
        assertEquals(0, BigDecimal(500).compareTo(assets.closingTotal))
        assertEquals(0, assets.lines.sumOf { line -> line.closingBalance }.compareTo(assets.closingTotal))
    }

    @Test
    fun `net worth is assets minus liabilities including liabilities under other liabilities`() {
        currentBalances = mapOf(
            SystemAccount.CASH.code to BigDecimal(1000),
            SystemAccount.TRADE_PAYABLES.code to BigDecimal(100),
            SystemAccount.LOANS_PAYABLE.code to BigDecimal(300)
        )

        val balances = build(DailyBusinessSummaryRequest(day9, day10)).days.single().balances

        assertEquals(0, BigDecimal(1000).compareTo(balances.assets.closingTotal))
        assertEquals(0, BigDecimal(400).compareTo(balances.liabilities.closingTotal))
        assertEquals(0, BigDecimal(600).compareTo(balances.netWorth))
        assertEquals("Other liabilities", balances.liabilities.lines.last().accountName)
    }

    @Test
    fun `a day's change is its net movement and the range change is the sum of the days' changes`() {
        currentBalances = mapOf(SystemAccount.CASH.code to BigDecimal(1000), SystemAccount.TRADE_PAYABLES.code to BigDecimal(100))

        val dailyBalancesResponse = build(
            DailyBusinessSummaryRequest(day8, day10),
            movement(day8, SystemAccount.CASH, EntryType.DEBIT, 300),
            movement(day9, SystemAccount.CASH, EntryType.CREDIT, 120),
            movement(day9, SystemAccount.TRADE_PAYABLES, EntryType.CREDIT, 100)
        )

        assertEquals(listOf(300, -120), dailyBalancesResponse.days.map { day -> cashChange(day.balances.assets) })
        assertEquals(180, cashChange(dailyBalancesResponse.rangeBalances.assets))
        assertEquals(0, dailyBalancesResponse.days.sumOf { day -> day.balances.netWorthChange }.compareTo(dailyBalancesResponse.rangeBalances.netWorthChange))
        assertEquals(cashClosing(dailyBalancesResponse.days.last().balances.assets), cashClosing(dailyBalancesResponse.rangeBalances.assets))
    }

    @Test
    fun `a location session still reads organization-wide balances`() {
        currentBalances = mapOf(SystemAccount.CASH.code to BigDecimal(1000))
        val organizationResponse = build(DailyBusinessSummaryRequest(day9, day10))

        SessionContextProvider.setSession(SessionContext(
            organization = OrgSession(UUID.randomUUID(), "org-a", "UTC"),
            location = LocationSession(UUID.randomUUID(), "loc-1")
        ))
        val locationResponse = build(DailyBusinessSummaryRequest(day9, day10))

        assertEquals(organizationResponse, locationResponse)
        assertNull(locationResponse.days.single().balances.assets.lines.firstOrNull { line -> line.accountCode == SystemAccount.WAGES_PAYABLE.code })
    }

    @Test
    fun `a closing balance on the wrong side is irregular for any tracked account, not only inventory`() {
        currentBalances = mapOf(
            SystemAccount.CASH.code to BigDecimal(-50),
            SystemAccount.INVENTORY.code to BigDecimal(-10),
            SystemAccount.TRADE_PAYABLES.code to BigDecimal(-30),
            SystemAccount.TRADE_RECEIVABLES.code to BigDecimal(70)
        )

        val balances = build(DailyBusinessSummaryRequest(day9, day10)).days.single().balances

        fun closingEffectOf(section: BalanceSection, account: SystemAccount) =
            section.lines.single { line -> line.accountCode == account.code }.closingBalanceEffect
        assertEquals(BalanceEffect.IRREGULAR, closingEffectOf(balances.assets, SystemAccount.CASH))
        assertEquals(BalanceEffect.IRREGULAR, closingEffectOf(balances.assets, SystemAccount.INVENTORY))
        assertEquals(BalanceEffect.IRREGULAR, closingEffectOf(balances.liabilities, SystemAccount.TRADE_PAYABLES))
        assertEquals(BalanceEffect.ADDS, closingEffectOf(balances.assets, SystemAccount.TRADE_RECEIVABLES))
    }

    @Test
    fun `a change is judged by its effect on net worth, so a liability falling adds and an asset falling subtracts`() {
        currentBalances = mapOf(SystemAccount.CASH.code to BigDecimal(1000), SystemAccount.TRADE_PAYABLES.code to BigDecimal(100))

        val balances = build(
            DailyBusinessSummaryRequest(day9, day10),
            movement(day9, SystemAccount.CASH, EntryType.CREDIT, 120),
            movement(day9, SystemAccount.TRADE_PAYABLES, EntryType.DEBIT, 40)
        ).days.single().balances

        assertEquals(BalanceEffect.SUBTRACTS, balances.assets.lines.single { line -> line.accountCode == SystemAccount.CASH.code }.changeEffect)
        assertEquals(BalanceEffect.ADDS, balances.liabilities.lines.single { line -> line.accountCode == SystemAccount.TRADE_PAYABLES.code }.changeEffect)
        assertEquals(BalanceEffect.ZERO, balances.assets.lines.single { line -> line.accountCode == SystemAccount.INVENTORY.code }.changeEffect)
        assertEquals(BalanceEffect.SUBTRACTS, balances.netWorthChangeEffect)
    }

    private fun build(
        dailyBusinessSummaryRequest: DailyBusinessSummaryRequest,
        vararg movements: LedgerAccountMovement,
        tree: List<AccountTreeNode> = RecordingAccountService.accountTree()
    ): DailyBalancesResponse {
        val accountService = mock(AccountService::class.java)
        `when`(accountService.getAccountTree()).thenReturn(tree)
        `when`(accountService.getFreshBalancesByCodes(requestedCodes(tree))).thenReturn(currentBalances)
        val reader = mock(LedgerMovementReader::class.java) { movements.toList() }
        return DailyBalancesService(accountService, reader).buildBalances(dailyBusinessSummaryRequest, asOf)
    }

    private fun requestedCodes(tree: List<AccountTreeNode>) =
        tree.filter { accountNode -> accountNode.isLeaf && accountNode.category.name in setOf("ASSET", "LIABILITY") }
            .map { accountNode -> accountNode.code }.toSet()

    private fun movement(date: LocalDate, account: SystemAccount, entryType: EntryType, amount: Int) =
        LedgerAccountMovement(date, account.code, entryType, BigDecimal(amount))

    private fun cashClosing(section: BalanceSection): Int = section.lines.first { line -> line.accountCode == SystemAccount.CASH.code }.closingBalance.toInt()

    private fun cashChange(section: BalanceSection): Int = section.lines.first { line -> line.accountCode == SystemAccount.CASH.code }.change.toInt()
}
