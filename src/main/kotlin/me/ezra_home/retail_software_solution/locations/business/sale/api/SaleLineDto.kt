package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.math.BigDecimal
import java.util.UUID

data class SaleLineDto(
    val id: UUID,
    val locationProductId: UUID,
    val productLabel: String,
    val productPrintLabel: String,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
    val lineTotal: BigDecimal,
    val defaultSalePrice: BigDecimal,
)
