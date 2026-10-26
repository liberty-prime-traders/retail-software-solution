package me.ezra_home.retail_software_solution.locations.business.opening_stock

import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockLineDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

object OpeningStockValidator {

    fun guardNotEmpty(lines: List<OpeningStockLineDto>) {
        if (lines.isEmpty()) throw RtsGenericException("Opening stock declaration must have at least one line")
    }

    fun guardNoDuplicateProducts(lines: List<OpeningStockLineDto>) {
        val duplicates = lines.groupingBy { it.locationProductId }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) throw RtsGenericException("Duplicate products in opening stock declaration: $duplicates")
    }

    fun guardNotAlreadyDeclared(
        alreadyDeclaredProductIds: Set<UUID>,
        requestedProductIds: Collection<UUID>,
        labelsByProductId: Map<UUID, String>
    ) {
        val conflicts = requestedProductIds.filter { it in alreadyDeclaredProductIds }
        if (conflicts.isNotEmpty()) {
            val labels = conflicts.map { " - ${labelsByProductId.getValue(it)}" }
            throw RtsGenericException("Opening stock already declared for the products below:", labels)
        }
    }
}
