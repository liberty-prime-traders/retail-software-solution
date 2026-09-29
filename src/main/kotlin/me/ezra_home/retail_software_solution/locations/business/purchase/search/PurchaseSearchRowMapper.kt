package me.ezra_home.retail_software_solution.locations.business.purchase.search

import jakarta.persistence.Tuple
import java.math.BigDecimal
import java.util.UUID

object PurchaseSearchRowMapper {

  fun paymentStatusSummaryFromTuple(tuple: Tuple): PurchasePaymentStatusSummaryRawRow = PurchasePaymentStatusSummaryRawRow(
    paymentStatusCode = tuple.get("payment_status", String::class.java),
    purchaseCount = tuple.get("purchase_count", Number::class.java).toLong(),
    totalOrdered = tuple.get("total_ordered", BigDecimal::class.java),
    totalPaid = tuple.get("total_paid", BigDecimal::class.java)
  )

  fun supplierSummaryFromTuple(tuple: Tuple): PurchaseSupplierSummaryRawRow = PurchaseSupplierSummaryRawRow(
    supplierId = tuple.get("supplier_id", UUID::class.java),
    purchaseCount = tuple.get("purchase_count", Number::class.java).toLong(),
    totalOrdered = tuple.get("total_ordered", BigDecimal::class.java),
    totalPaid = tuple.get("total_paid", BigDecimal::class.java)
  )
}
