package me.ezra_home.retail_software_solution.cross_tier.expense.kafka_handler

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseReissuer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class ExpenseReissuerResolverTest {

    private val locationExpenseReissuer = mock(ExpenseReissuer::class.java).also { `when`(it.isLocationLevel).thenReturn(true) }
    private val orgExpenseReissuer = mock(ExpenseReissuer::class.java).also { `when`(it.isLocationLevel).thenReturn(false) }
    private val expenseReissuerResolver = ExpenseReissuerResolver(listOf(locationExpenseReissuer, orgExpenseReissuer))

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a location in session selects the location tier`() {
        SessionContextProvider.getSession().location = LocationSession(UUID.randomUUID(), "loc-1", "UTC")

        assertSame(locationExpenseReissuer, expenseReissuerResolver.current())
    }

    @Test
    fun `clearing the session location selects the org tier again`() {
        SessionContextProvider.getSession().location = LocationSession(UUID.randomUUID(), "loc-1", "UTC")
        SessionContextProvider.getSession().location = null

        assertSame(orgExpenseReissuer, expenseReissuerResolver.current())
    }
}
