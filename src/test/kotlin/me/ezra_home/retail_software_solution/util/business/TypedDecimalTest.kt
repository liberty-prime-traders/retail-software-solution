package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class TypedDecimalTest {

    private val one = BigDecimal.ONE

    @Test
    fun `values of different typed decimal types are never equal, even with the same number`() {
        assertNotEquals(Money.of(one) as Any, PositiveMoney.of(one) as Any)
        assertNotEquals(UserProvidedQuantity.of(one) as Any, PositiveUserProvidedQuantity.of(one) as Any)
        assertNotEquals(Money.of(one) as Any, UserProvidedQuantity.of(one) as Any)
    }

    @Test
    fun `the same type with the same number is equal and compares by value`() {
        assertEquals(PositiveMoney.of(BigDecimal("1.5")), PositiveMoney.of(BigDecimal("1.50")))
        assertEquals(-1, PositiveMoney.of(one).compareTo(PositiveMoney.of(BigDecimal("2"))))
    }
}
