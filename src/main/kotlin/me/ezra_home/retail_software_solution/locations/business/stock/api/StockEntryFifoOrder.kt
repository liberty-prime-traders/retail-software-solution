package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import java.time.OffsetDateTime

private val nullsLastInstant: Comparator<OffsetDateTime?> = nullsLast()

val stockEntryFifoComparator: Comparator<StockEntryEntity> = Comparator { a, b ->
    val byPriority = a.priority.compareTo(b.priority)
    if (byPriority != 0) byPriority else nullsLastInstant.compare(a.createdOn, b.createdOn)
}
