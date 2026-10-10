package me.ezra_home.retail_software_solution.organizations.business.ledger

import java.math.BigDecimal

interface SubledgerAmountTotals {
    val contactReferenceNumber: String
    val payableTotal: BigDecimal
    val receivableTotal: BigDecimal
}
