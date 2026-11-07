package me.ezra_home.retail_software_solution.locations.business.stock

val stockEntryFifoComparator: Comparator<StockEntryEntity> =
    compareBy<StockEntryEntity> { stockEntryEntity: StockEntryEntity -> stockEntryEntity.priority }
        .thenBy(nullsLast()) { stockEntryEntity: StockEntryEntity -> stockEntryEntity.activatedOn }
        .thenBy(nullsLast()) { stockEntryEntity: StockEntryEntity -> stockEntryEntity.createdOn }
