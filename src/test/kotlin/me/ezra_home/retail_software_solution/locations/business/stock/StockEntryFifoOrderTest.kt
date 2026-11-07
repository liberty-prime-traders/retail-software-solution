package me.ezra_home.retail_software_solution.locations.business.stock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockEntryFifoOrderTest {

    private val locationProductId = UUID.randomUUID()

    private fun stockEntry(
        name: String,
        activatedOn: OffsetDateTime?,
        createdOn: OffsetDateTime? = activatedOn,
        priority: Int = 0
    ) = StockEntryEntity(
        locationProductId = locationProductId,
        detailReference = name,
        storageBatchSize = BigDecimal.TEN,
        storageQuantityRemaining = BigDecimal.TEN,
        priority = priority,
        unitCost = BigDecimal.ONE,
        batchCost = BigDecimal.TEN,
        costRemaining = BigDecimal.TEN,
        activatedOn = activatedOn,
    ).also { it.createdOn = createdOn }

    private fun at(day: Int) = OffsetDateTime.parse("2026-10-%02dT08:00:00Z".format(day))

    @Test
    fun `a replacement with the revised activation date takes the revised layer's slot however late it was made`() {
        val firstDelivery = stockEntry("first", at(1))
        val secondDelivery = stockEntry("second", at(2))
        val fourthDelivery = stockEntry("fourth", at(4))
        val replacementOfThird = stockEntry("replacement-of-third", activatedOn = at(3), createdOn = at(20))

        val sorted = listOf(
            fourthDelivery,
            replacementOfThird,
            firstDelivery,
            secondDelivery
        ).sortedWith(stockEntryFifoComparator)

        assertEquals(listOf("first", "second", "replacement-of-third", "fourth"), sorted.map { it.detailReference })
    }

    @Test
    fun `replacements made weeks apart each keep their own slot`() {
        val replacementOfSecond = stockEntry("replacement-of-second", activatedOn = at(2), createdOn = at(25))
        val replacementOfFirst = stockEntry("replacement-of-first", activatedOn = at(1), createdOn = at(28))
        val thirdDelivery = stockEntry("third", at(3))

        val sorted = listOf(thirdDelivery, replacementOfFirst, replacementOfSecond).sortedWith(stockEntryFifoComparator)

        assertEquals(
            listOf("replacement-of-first", "replacement-of-second", "third"),
            sorted.map { it.detailReference }
        )
    }

    @Test
    fun `priority still outranks the activation date`() {
        val early = stockEntry("early", at(1), priority = 1)
        val late = stockEntry("late", at(9), priority = 0)

        val sorted = listOf(early, late).sortedWith(stockEntryFifoComparator)

        assertEquals(listOf("late", "early"), sorted.map { it.detailReference })
    }

    @Test
    fun `layers with the same activation date fall back to when they were created`() {
        val createdLater = stockEntry("created-later", activatedOn = at(1), createdOn = at(6))
        val createdEarlier = stockEntry("created-earlier", activatedOn = at(1), createdOn = at(5))

        val sorted = listOf(createdLater, createdEarlier).sortedWith(stockEntryFifoComparator)

        assertEquals(listOf("created-earlier", "created-later"), sorted.map { it.detailReference })
    }
}
