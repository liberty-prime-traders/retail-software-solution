package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.math.RoundingMode

object LinePricing {

    fun lineTotal(
        defaultSalePrice: BigDecimal,
        userProvidedQuantity: BigDecimal,
        storageUnitsPerUnit: Long,
        baseUnitSizeInRoot: Long
    ): BigDecimal =
        defaultSalePrice.multiply(userProvidedQuantity)
            .multiply(BigDecimal.valueOf(storageUnitsPerUnit))
            .divide(BigDecimal.valueOf(baseUnitSizeInRoot), Decimals.MoneyScale.scale, RoundingMode.HALF_UP)

    fun overrideTotal(typedUnitPrice: BigDecimal, userProvidedQuantity: BigDecimal): BigDecimal =
        Decimals.MoneyScale.multiply(typedUnitPrice, userProvidedQuantity)
}
