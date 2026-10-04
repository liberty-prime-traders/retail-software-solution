package me.ezra_home.retail_software_solution.locations.business.sale.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object SaleSearchRowMapper {

  fun fromTuple(tuple: Tuple): SaleSearchRawRow = SaleSearchRawRow(
    id = tuple.get("id", UUID::class.java),
    referenceNumber = tuple.get("reference_number", String::class.java),
    contactId = tuple.get("contact_id", UUID::class.java),
    soldByUserId = tuple.get("sold_by_id", UUID::class.java),
    dateSold = DateQualifier.toOffsetDateTime(tuple.get("date_sold", Instant::class.java)),
    createdOn = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("created_on", Instant::class.java))),
    status = SaleStatus.entries.first { it.code == tuple.get("status", String::class.java) },
    paymentStatus = PaymentStatus.entries.first { it.code == tuple.get("payment_status", String::class.java) },
    receivableTotal = tuple.get("receivable_total", BigDecimal::class.java)
  )

  fun summaryFromTuple(tuple: Tuple): SaleStatusSummaryRawRow = SaleStatusSummaryRawRow(
    status = SaleStatus.entries.first { it.code == tuple.get("status", String::class.java) },
    saleCount = tuple.get("sale_count", Number::class.java).toLong(),
    receivableTotal = tuple.get("receivable_total", BigDecimal::class.java),
    discountTotal = tuple.get("discount_total", BigDecimal::class.java),
    paidTotal = tuple.get("paid_total", BigDecimal::class.java),
    outstandingTotal = tuple.get("outstanding_total", BigDecimal::class.java),
    creditTotal = tuple.get("credit_total", BigDecimal::class.java)
  )
}
