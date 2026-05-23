package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionLine(
    val identity: SessionIdentity,
    override val locationProductId: UUID,
    val productLabel: String,
    override val quantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
    val defaultSalePrice: BigDecimal,

) : ProductLineWithPrice {
    override val unitPrice: BigDecimal = Decimals.multiplyScale4(defaultSalePrice, conversionFactor)
}
