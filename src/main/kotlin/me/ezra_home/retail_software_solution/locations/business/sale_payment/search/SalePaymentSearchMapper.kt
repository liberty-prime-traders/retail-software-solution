package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchResultDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentStatusFilter
import java.util.UUID

object SalePaymentSearchMapper {

  fun toRowDto(
    row: SalePaymentSearchRawRow,
    paymentMethodNamesById: Map<UUID, String>,
    contactNamesById: Map<UUID, String>
  ): SalePaymentSearchResultDto = SalePaymentSearchResultDto(
    id = row.id,
    referenceNumber = row.referenceNumber,
    createdOn = row.createdOn,
    paymentDate = row.paymentDate,
    saleId = row.saleId,
    saleReferenceNumber = row.saleReferenceNumber,
    contactId = row.contactId,
    customerName = contactNamesById.getValue(row.contactId),
    paymentMethodId = row.paymentMethodId,
    paymentMethodName = paymentMethodNamesById.getValue(row.paymentMethodId),
    amount = row.amount,
    reference = row.reference,
    status = if (row.voidReason != null) SalePaymentStatusFilter.VOIDED else SalePaymentStatusFilter.ACTIVE,
    voidReason = row.voidReason
  )
}
