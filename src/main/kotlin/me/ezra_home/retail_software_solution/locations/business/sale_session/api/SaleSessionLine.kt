package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.util.business.Decimals
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
    val storageUnitsPerUserProvidedUnit: Long,
    override val lineTotal: BigDecimal,
    val defaultSalePrice: BigDecimal,
    val storageQuantityOnHand: BigDecimal = BigDecimal.ZERO,
    val storageQuantityReserved: BigDecimal = BigDecimal.ZERO,
    val storageQuantityAvailable: BigDecimal = BigDecimal.ZERO,

) : ProductLineWithPrice {

    companion object {
        // Multiplies first and divides last: multiply is exact, so the single division is the only rounding.
        fun lineTotal(defaultSalePrice: BigDecimal, lineQuantities: LineQuantities): BigDecimal =
            defaultSalePrice.multiply(lineQuantities.userProvidedQuantity)
                .multiply(BigDecimal.valueOf(lineQuantities.storageUnitsPerUserProvidedUnit))
                .let { storageValue -> Decimals.MoneyScale.divide(storageValue, BigDecimal.valueOf(lineQuantities.storageUnitsPerBaseUnit)) }
    }
}
