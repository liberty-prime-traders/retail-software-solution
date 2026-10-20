package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionLine(
    val identity: SessionIdentity,
    override val locationProductId: UUID,
    val productLabel: String,
    val productPrintLabel: String,
    override val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val storageUnitsPerUnit: Long,
    override val lineTotal: BigDecimal,
    val defaultSalePrice: BigDecimal,
    val storageQuantityOnHand: BigDecimal = BigDecimal.ZERO,
    val storageQuantityReserved: BigDecimal = BigDecimal.ZERO,
    val storageQuantityAvailable: BigDecimal = BigDecimal.ZERO,

) : ProductLineWithPrice
