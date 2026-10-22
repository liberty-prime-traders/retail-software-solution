package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.math.RoundingMode

object LinePricing {

    // default_sale_price is per base unit: one division at the end, so 1 Bale and 18 Pieces of the same product total the same
    fun lineTotal(
        defaultSalePrice: BigDecimal,
        userProvidedQuantity: BigDecimal,
        storageUnitsPerUnit: Long,
        baseUnitSizeInRoot: Long
    ): BigDecimal =
        defaultSalePrice.multiply(userProvidedQuantity).multiply(BigDecimal.valueOf(storageUnitsPerUnit))
            .divide(BigDecimal.valueOf(baseUnitSizeInRoot), Decimals.Scale2.scale, RoundingMode.HALF_UP)

    fun overrideTotal(typedUnitPrice: BigDecimal, userProvidedQuantity: BigDecimal): BigDecimal =
        Decimals.Scale2.multiply(typedUnitPrice, userProvidedQuantity)
}
