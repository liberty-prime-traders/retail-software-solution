package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class UnitChainResolverTest {

    private val gram = UnitChainNode(UUID.randomUUID(), null, null)
    private val kilogram = UnitChainNode(UUID.randomUUID(), gram.id, 1000L)
    private val tonne = UnitChainNode(UUID.randomUUID(), kilogram.id, 1000L)

    @Test
    fun `a chain multiplies its factors down to the root`() {
        val placements = UnitChainResolver.resolvePlacements(listOf(tonne, gram, kilogram))

        assertEquals(UnitPlacement(gram.id, 1_000_000L), placements.getValue(tonne.id))
        assertEquals(UnitPlacement(gram.id, 1_000L), placements.getValue(kilogram.id))
        assertEquals(UnitPlacement(gram.id, 1L), placements.getValue(gram.id))
    }

    @Test
    fun `a unit with a factor of 1 is valid and two equal-sized units are allowed`() {
        val wrapper = UnitChainNode(UUID.randomUUID(), gram.id, 1L)
        val sibling = UnitChainNode(UUID.randomUUID(), gram.id, 1L)

        val placements = UnitChainResolver.resolvePlacements(listOf(gram, wrapper, sibling))

        assertEquals(1L, placements.getValue(wrapper.id).sizeInRoot)
        assertEquals(1L, placements.getValue(sibling.id).sizeInRoot)
    }

    @Test
    fun `a self reference and a cycle are rejected`() {
        val selfReferencing = UnitChainNode(UUID.randomUUID(), null, 2L).let { it.copy(baseUnitId = it.id) }
        val first = UnitChainNode(UUID.randomUUID(), null, 2L)
        val second = UnitChainNode(UUID.randomUUID(), first.id, 2L)

        assertThrows(RtsGenericException::class.java) { UnitChainResolver.resolvePlacements(listOf(selfReferencing)) }
        assertThrows(RtsGenericException::class.java) {
            UnitChainResolver.resolvePlacements(listOf(first.copy(baseUnitId = second.id), second))
        }
    }

    @Test
    fun `a base unit that does not exist is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            UnitChainResolver.resolvePlacements(listOf(UnitChainNode(UUID.randomUUID(), UUID.randomUUID(), 2L)))
        }
    }

    @Test
    fun `a size above one billion is rejected and one billion exactly is accepted`() {
        val atLimit = UnitChainNode(UUID.randomUUID(), gram.id, UnitChainResolver.MAX_SIZE_IN_ROOT)
        val overLimit = UnitChainNode(UUID.randomUUID(), gram.id, UnitChainResolver.MAX_SIZE_IN_ROOT + 1)

        assertEquals(UnitChainResolver.MAX_SIZE_IN_ROOT, UnitChainResolver.resolvePlacements(listOf(gram, atLimit)).getValue(atLimit.id).sizeInRoot)
        assertThrows(RtsGenericException::class.java) { UnitChainResolver.resolvePlacements(listOf(gram, overLimit)) }
    }

    @Test
    fun `a chain whose product overflows Long is rejected`() {
        val huge = UnitChainNode(UUID.randomUUID(), gram.id, Long.MAX_VALUE)
        val hugerStill = UnitChainNode(UUID.randomUUID(), huge.id, 3L)

        assertThrows(RtsGenericException::class.java) { UnitChainResolver.resolvePlacements(listOf(gram, huge, hugerStill)) }
    }

    @Test
    fun `the largest user quantity times the largest size fits numeric 28 4`() {
        val largestQuantity = BigDecimal("999999999999999.9999")
        val largestStorageQuantity = largestQuantity.multiply(BigDecimal(UnitChainResolver.MAX_SIZE_IN_ROOT))

        assertEquals(24, largestStorageQuantity.setScale(0, RoundingMode.DOWN).toPlainString().length)
        assertEquals(true, largestStorageQuantity.precision() <= 28)
    }
}
