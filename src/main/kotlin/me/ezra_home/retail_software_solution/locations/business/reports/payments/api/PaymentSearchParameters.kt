package me.ezra_home.retail_software_solution.locations.business.reports.payments.api

import me.ezra_home.retail_software_solution.util.business.StringUtils
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class PaymentVoidStatusFilter {
  ACTIVE,
  VOIDED
}

class PaymentSearchParameters(
  val types: Set<PaymentType> = PaymentType.entries.toSet(),
  val recordedFrom: OffsetDateTime? = null,
  val recordedBefore: OffsetDateTime? = null,
  val paymentDateFrom: OffsetDateTime? = null,
  val paymentDateBefore: OffsetDateTime? = null,
  val contactIds: List<UUID> = emptyList(),
  val paymentMethodIds: List<UUID> = emptyList(),
  val statuses: Set<PaymentVoidStatusFilter> = emptySet(),
  val minAmount: BigDecimal? = null,
  val maxAmount: BigDecimal? = null,
  referenceNumbersByType: Map<PaymentType, List<String>> = emptyMap()
) {

  val referenceNumbersByType: Map<PaymentType, List<String>> = referenceNumbersByType
    .mapValues { (_, referenceNumbers) -> StringUtils.dropBlank(referenceNumbers) }
    .filterValues { it.isNotEmpty() }

  fun referenceNumbersOf(type: PaymentType): List<String> = referenceNumbersByType[type].orEmpty()

  // A reference filter belongs to one type, so setting any reference filter drops the types that have none.
  fun includes(type: PaymentType): Boolean =
    type in types && (referenceNumbersByType.isEmpty() || referenceNumbersOf(type).isNotEmpty())
}
