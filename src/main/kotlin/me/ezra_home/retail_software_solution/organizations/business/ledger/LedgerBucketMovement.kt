package me.ezra_home.retail_software_solution.organizations.business.ledger

import java.math.BigDecimal

interface LedgerBucketMovement {
    val bucket: Int
    val accountCode: String
    val entryTypeCode: String
    val sourceTypeCode: String
    val amount: BigDecimal
}

interface LedgerAccountBucketMovement {
    val bucket: Int
    val accountCode: String
    val entryTypeCode: String
    val amount: BigDecimal
}
