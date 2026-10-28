package me.ezra_home.retail_software_solution.organizations.business.daily_business_summary

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyBusinessSummaryRequest
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyPerformanceFigures
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyPerformanceResponse
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.DailyPerformanceService
import me.ezra_home.retail_software_solution.organizations.business.daily_business_summary.api.PeriodStatus
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovement
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovementReader
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class DailyPerformanceServiceTest {

    private val digitalWalletCode = "001.001.005.001"
    private val accountTree = RecordingAccountService.accountTree(digitalWalletCode)
    private val accountService = mock(AccountService::class.java).also { mockedAccountService -> `when`(mockedAccountService.getAccountTree()).thenReturn(accountTree) }
    private val asOf = OffsetDateTime.of(2026, 3, 10, 12, 0, 0, 0, ZoneOffset.UTC)
    private val day8 = LocalDate.of(2026, 3, 8)
    private val day9 = LocalDate.of(2026, 3, 9)
    private val day10 = LocalDate.of(2026, 3, 10)
    private val dailyBusinessSummaryRequest = DailyBusinessSummaryRequest(from = day8, before = LocalDate.of(2026, 3, 11))

    private var requestedLocationId: UUID? = null
    private var requestedSources: Set<LedgerSourceType> = emptySet()

    @BeforeEach
    fun setOrganizationSession() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(UUID.randomUUID(), "org-a", "UTC")))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a plain sale with a discount and cost of goods sold yields net sales, gross profit and billed amount`() {
        val dailyPerformanceResponse = perform(
            sale(day9, EntryType.CREDIT to SystemAccount.GROSS_SALES to 100, EntryType.DEBIT to SystemAccount.SALES_DISCOUNTS to 10,
                EntryType.DEBIT to SystemAccount.TRADE_RECEIVABLES to 90, EntryType.DEBIT to SystemAccount.COST_OF_GOODS_SOLD to 60,
                EntryType.CREDIT to SystemAccount.INVENTORY to 60)
        )

        val figures = figuresOn(dailyPerformanceResponse, day9)
        assertAmount(100, figures.grossSales)
        assertAmount(10, figures.discounts)
        assertAmount(90, figures.netSales)
        assertAmount(60, figures.costOfGoodsSold)
        assertAmount(30, figures.grossProfit)
        assertEquals(BigDecimal("33.3"), figures.grossMarginPercent)
        assertAmount(30, figures.netProfit)
        assertAmount(90, figures.billedToCustomers)
    }

    @Test
    fun `tax billed separately leaves sales untouched, tax included in the taxable amount debits gross sales, tax as expense is an expense line`() {
        val dailyPerformanceResponse = perform(
            sale(day8, EntryType.CREDIT to SystemAccount.GROSS_SALES to 100, EntryType.CREDIT to SystemAccount.TAX_PAYABLE to 8),
            sale(day9, EntryType.CREDIT to SystemAccount.GROSS_SALES to 100, EntryType.DEBIT to SystemAccount.GROSS_SALES to 8),
            sale(day10, EntryType.CREDIT to SystemAccount.GROSS_SALES to 100, EntryType.DEBIT to SystemAccount.TAX_EXPENSE to 8)
        )

        assertAmount(100, figuresOn(dailyPerformanceResponse, day8).grossSales)
        assertAmount(92, figuresOn(dailyPerformanceResponse, day9).grossSales)
        val taxExpenseLine = figuresOn(dailyPerformanceResponse, day10).expenseLines.single()
        assertEquals(SystemAccount.TAX_EXPENSE.code, taxExpenseLine.accountCode)
        assertAmount(8, taxExpenseLine.amount)
        assertAmount(92, figuresOn(dailyPerformanceResponse, day10).netProfit)
    }

    @Test
    fun `a sale void processed on a later day reduces that later day only`() {
        val dailyPerformanceResponse = perform(
            sale(day8, EntryType.CREDIT to SystemAccount.GROSS_SALES to 100),
            movements(day9, LedgerSourceType.SALE_VOID, EntryType.DEBIT to SystemAccount.GROSS_SALES to 100)
        )

        assertAmount(100, figuresOn(dailyPerformanceResponse, day8).netSales)
        assertAmount(-100, figuresOn(dailyPerformanceResponse, day9).netSales)
        assertAmount(0, dailyPerformanceResponse.totals.netSales)
    }

    @Test
    fun `an expense and its void land on their own days and cancel in the totals`() {
        val dailyPerformanceResponse = perform(
            movements(day8, LedgerSourceType.EXPENSE, EntryType.DEBIT to SystemAccount.RENT_EXPENSE to 50),
            movements(day9, LedgerSourceType.EXPENSE_VOID, EntryType.CREDIT to SystemAccount.RENT_EXPENSE to 50)
        )

        assertAmount(50, figuresOn(dailyPerformanceResponse, day8).expensesTotal)
        assertAmount(-50, figuresOn(dailyPerformanceResponse, day9).expenseLines.single().amount)
        assertTrue(dailyPerformanceResponse.totals.expenseLines.isEmpty())
        assertAmount(0, dailyPerformanceResponse.totals.netProfit)
    }

    @Test
    fun `stock adjustments feed expense lines and free goods reduce cost of goods sold`() {
        val dailyPerformanceResponse = perform(
            sale(day8, EntryType.DEBIT to SystemAccount.COST_OF_GOODS_SOLD to 60),
            movements(day8, LedgerSourceType.STOCK_ADJUSTMENT, EntryType.DEBIT to SystemAccount.INTERNAL_USE_EXPENSE to 20,
                EntryType.CREDIT to SystemAccount.FREE_GOODS_RECEIVED to 12),
            movements(day9, LedgerSourceType.STOCK_ADJUSTMENT, EntryType.DEBIT to SystemAccount.SHRINKAGE_AND_LOSSES to 15),
            movements(day10, LedgerSourceType.STOCK_ADJUSTMENT, EntryType.CREDIT to SystemAccount.SHRINKAGE_AND_LOSSES to 30)
        )

        assertAmount(48, figuresOn(dailyPerformanceResponse, day8).costOfGoodsSold)
        assertEquals(listOf(SystemAccount.INTERNAL_USE_EXPENSE.code), figuresOn(dailyPerformanceResponse, day8).expenseLines.map { expenseLine -> expenseLine.accountCode })
        assertAmount(15, figuresOn(dailyPerformanceResponse, day9).expenseLines.single().amount)
        assertAmount(-30, figuresOn(dailyPerformanceResponse, day10).expenseLines.single().amount)
        assertTrue(dailyPerformanceResponse.totals.expenseLines.none { expenseLine -> expenseLine.accountCode == SystemAccount.COST_OF_GOODS_SOLD.code })
    }

    @Test
    fun `only profit and loss and collection sources are requested`() {
        perform()

        assertEquals(
            setOf(
                LedgerSourceType.SALE, LedgerSourceType.SALE_VOID, LedgerSourceType.EXPENSE, LedgerSourceType.EXPENSE_VOID,
                LedgerSourceType.STOCK_ADJUSTMENT, LedgerSourceType.SALE_PAYMENT, LedgerSourceType.SALE_PAYMENT_VOID
            ),
            requestedSources
        )
    }

    @Test
    fun `sale payments are collected by payment account and a payment void takes the amount back`() {
        val dailyPerformanceResponse = perform(
            movements(day9, LedgerSourceType.SALE_PAYMENT, EntryType.DEBIT to SystemAccount.CASH to 40,
                EntryType.DEBIT to digitalWalletCode to 50, EntryType.CREDIT to SystemAccount.TRADE_RECEIVABLES to 90),
            movements(day9, LedgerSourceType.SALE_PAYMENT_VOID, EntryType.CREDIT to SystemAccount.CASH to 40,
                EntryType.DEBIT to SystemAccount.TRADE_RECEIVABLES to 40)
        )

        val figures = figuresOn(dailyPerformanceResponse, day9)
        assertEquals(listOf(digitalWalletCode), figures.collectedByAccount.map { collectedLine -> collectedLine.accountCode })
        assertAmount(50, figures.collectedTotal)
        assertAmount(0, figures.billedToCustomers)
    }

    @Test
    fun `a location session reads one location and an organization session reads all of them`() {
        perform()
        assertNull(requestedLocationId)

        val locationId = UUID.randomUUID()
        SessionContextProvider.setSession(SessionContext(
            organization = OrgSession(UUID.randomUUID(), "org-a", "UTC"),
            location = LocationSession(locationId, "loc-1")
        ))
        perform()
        assertEquals(locationId, requestedLocationId)
    }

    @Test
    fun `every date in the half-open range is present and margins are null without net sales`() {
        val dailyPerformanceResponse = perform(sale(day8, EntryType.CREDIT to SystemAccount.GROSS_SALES to 100))

        assertEquals(listOf(day8, day9, day10), dailyPerformanceResponse.days.map { day -> day.date })
        assertNull(figuresOn(dailyPerformanceResponse, day9).grossMarginPercent)
        assertNull(figuresOn(dailyPerformanceResponse, day9).netMarginPercent)
    }

    @Test
    fun `totals recompute margins from the summed amounts instead of averaging`() {
        val dailyPerformanceResponse = perform(
            sale(day8, EntryType.CREDIT to SystemAccount.GROSS_SALES to 100, EntryType.DEBIT to SystemAccount.COST_OF_GOODS_SOLD to 50),
            sale(day9, EntryType.CREDIT to SystemAccount.GROSS_SALES to 300, EntryType.DEBIT to SystemAccount.COST_OF_GOODS_SOLD to 270)
        )

        assertEquals(BigDecimal("50.0"), figuresOn(dailyPerformanceResponse, day8).grossMarginPercent)
        assertEquals(BigDecimal("10.0"), figuresOn(dailyPerformanceResponse, day9).grossMarginPercent)
        assertEquals(BigDecimal("20.0"), dailyPerformanceResponse.totals.grossMarginPercent)
    }

    @Test
    fun `status is in progress only for the day containing asOf`() {
        val dailyPerformanceResponse = perform()

        assertEquals(listOf(PeriodStatus.COMPLETE, PeriodStatus.COMPLETE, PeriodStatus.IN_PROGRESS), dailyPerformanceResponse.days.map { day -> day.status })
        assertEquals(PeriodStatus.IN_PROGRESS, dailyPerformanceResponse.rangeStatus)
        val pastResponse = DailyPerformanceService(accountService, readerReturning(emptyList()))
            .buildPerformance(DailyBusinessSummaryRequest(day8, day10), asOf)
        assertEquals(PeriodStatus.COMPLETE, pastResponse.rangeStatus)
    }

    @Test
    fun `range guard names the rule that was broken`() {
        fun message(from: LocalDate, before: LocalDate) = assertThrows(RtsGenericException::class.java) {
            perform(dailyBusinessSummaryRequest = DailyBusinessSummaryRequest(from, before))
        }.message

        assertTrue(message(day9, day9).contains("'from' must be before 'before'"))
        assertTrue(message(day9, LocalDate.of(2026, 3, 12)).contains("after tomorrow"))
        assertTrue(message(LocalDate.of(2025, 12, 1), day10).contains("92 days"))
        perform(dailyBusinessSummaryRequest = DailyBusinessSummaryRequest(LocalDate.of(2025, 12, 10), LocalDate.of(2026, 3, 11)))
    }

    private fun perform(vararg movements: List<LedgerMovement>, dailyBusinessSummaryRequest: DailyBusinessSummaryRequest = this.dailyBusinessSummaryRequest): DailyPerformanceResponse =
        DailyPerformanceService(accountService, readerReturning(movements.toList().flatten())).buildPerformance(dailyBusinessSummaryRequest, asOf)

    @Suppress("UNCHECKED_CAST")
    private fun readerReturning(movements: List<LedgerMovement>): LedgerMovementReader =
        mock(LedgerMovementReader::class.java) { invocation ->
            requestedSources = invocation.arguments[2] as Set<LedgerSourceType>
            requestedLocationId = invocation.arguments[3] as UUID?
            movements
        }

    private fun sale(date: LocalDate, vararg legs: Pair<Pair<EntryType, Any>, Int>) = movements(date, LedgerSourceType.SALE, *legs)

    private fun movements(date: LocalDate, source: LedgerSourceType, vararg legs: Pair<Pair<EntryType, Any>, Int>) =
        legs.map { (typeAndAccount, amount) ->
            val (entryType, account) = typeAndAccount
            LedgerMovement(date, if (account is SystemAccount) account.code else account as String, entryType, source, BigDecimal(amount))
        }

    private fun figuresOn(dailyPerformanceResponse: DailyPerformanceResponse, date: LocalDate): DailyPerformanceFigures =
        dailyPerformanceResponse.days.first { day -> day.date == date }.figures

    private fun assertAmount(expected: Int, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")
}
