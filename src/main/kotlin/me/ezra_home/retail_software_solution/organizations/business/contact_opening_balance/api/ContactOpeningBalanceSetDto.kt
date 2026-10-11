package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerBalanceDto
import java.math.BigDecimal
import java.util.Optional

data class ContactOpeningBalanceSetDto(
    val payable: Optional<BigDecimal>? = null,
    val receivable: Optional<BigDecimal>? = null
) {

    fun applyTo(existing: SubledgerBalanceDto): SubledgerBalanceDto = SubledgerBalanceDto(
        payable = payable?.orElse(BigDecimal.ZERO) ?: existing.payable,
        receivable = receivable?.orElse(BigDecimal.ZERO) ?: existing.receivable
    )
}
