package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import java.math.BigDecimal

object SaleArrears {

    fun arrearsTotal(status: SaleStatus, receivableTotal: BigDecimal, totalPaid: BigDecimal): BigDecimal =
        if (status == SaleStatus.CONFIRMED) receivableTotal - totalPaid else BigDecimal.ZERO
}
