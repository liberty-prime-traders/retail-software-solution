package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerBalanceDto
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.Money
import java.math.BigDecimal
import java.util.Optional

data class ContactOpeningBalanceSetDto(
    val payable: Optional<Money>? = null,
    val receivable: Optional<Money>? = null
) {

    fun applyTo(existing: SubledgerBalanceDto): SubledgerBalanceDto = SubledgerBalanceDto(
        payable = payable?.map { it.toBigDecimal() }?.orElse(Decimals.MoneyScale.round(BigDecimal.ZERO)) ?: existing.payable,
        receivable = receivable?.map { it.toBigDecimal() }?.orElse(Decimals.MoneyScale.round(BigDecimal.ZERO)) ?: existing.receivable
    )
}
