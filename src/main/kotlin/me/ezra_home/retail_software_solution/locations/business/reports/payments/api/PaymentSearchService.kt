package me.ezra_home.retail_software_solution.locations.business.reports.payments.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.reports.payments.PaymentMethodTotals
import me.ezra_home.retail_software_solution.locations.business.reports.payments.PaymentRow
import me.ezra_home.retail_software_solution.locations.business.reports.payments.PaymentSearchFetcher
import me.ezra_home.retail_software_solution.locations.business.reports.payments.PaymentSearchValidator
import me.ezra_home.retail_software_solution.locations.business.reports.payments.PaymentSource
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodDataFetcher
import me.ezra_home.retail_software_solution.util.business.mappers.NameResolution
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PaymentSearchService(
  private val paymentSearchFetcher: PaymentSearchFetcher,
  private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
  private val nameResolution: NameResolution
) {

  fun search(pageRequest: PageRequest<PaymentSearchParameters, String>): PageResponse<PaymentRowDto, String> {
    val paymentSearchParameters = pageRequest.parameters
    PaymentSearchValidator.guardValidParameters(paymentSearchParameters)
    PaymentSearchValidator.guardValidPageSize(pageRequest.requestedSize)
    val keysetSearchCursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

    val paymentRows = paymentSearchFetcher.search(
      includedSources(paymentSearchParameters), paymentSearchParameters, keysetSearchCursor, pageRequest.requestedSize
    )

    val pagePaymentRows = paymentRows.take(pageRequest.requestedSize)
    val paymentMethodNamesById = paymentMethodDataFetcher.getNamesById()
    val contactNamesById = nameResolution.organizationContacts(pagePaymentRows.map { it.contactId })

    return PageResponse(
      currentCursor = pagePaymentRows.lastOrNull()?.let { KeysetSearchCursor(it.recordedOn, it.id).encode() }
        ?: pageRequest.previousCursor,
      hasMore = paymentRows.size > pageRequest.requestedSize,
      contents = pagePaymentRows.map { toRowDto(it, paymentMethodNamesById, contactNamesById) }
    )
  }

  fun summarize(paymentSearchParameters: PaymentSearchParameters): PaymentSummaryResponseDto {
    PaymentSearchValidator.guardValidParameters(paymentSearchParameters)
    val paymentMethodNamesById = paymentMethodDataFetcher.getNamesById()

    return PaymentSummaryResponseDto(
      summaries = includedSources(paymentSearchParameters).map { paymentSource ->
        val paymentMethodTotals = paymentSearchFetcher.summarize(paymentSource, paymentSearchParameters)
        buildTypeSummary(paymentSource, paymentMethodTotals, paymentMethodNamesById)
      }
    )
  }

  // A deleted or unnamed payment method is absent from the names map, and its payments remain.
  private fun paymentMethodNameOf(paymentMethodId: UUID, paymentMethodNamesById: Map<UUID, String>) =
    paymentMethodNamesById[paymentMethodId] ?: paymentMethodId.toString()

  private fun includedSources(paymentSearchParameters: PaymentSearchParameters) =
    PaymentSource.entries.filter { paymentSearchParameters.includes(it.type) }

  private fun toRowDto(
    paymentRow: PaymentRow,
    paymentMethodNamesById: Map<UUID, String>,
    contactNamesById: Map<UUID, String>
  ) = PaymentRowDto(
    id = paymentRow.id,
    type = paymentRow.type,
    referenceNumber = paymentRow.referenceNumber,
    contactName = contactNamesById[paymentRow.contactId],
    paymentMethodName = paymentMethodNameOf(paymentRow.paymentMethodId, paymentMethodNamesById),
    amount = paymentRow.amount,
    balanceEffect = PaymentSource.forType(paymentRow.type).balanceEffect,
    paymentDate = paymentRow.paymentDate,
    recordedOn = paymentRow.recordedOn,
    voided = paymentRow.voidReason != null,
    voidedReason = paymentRow.voidReason
  )

  private fun buildTypeSummary(
    paymentSource: PaymentSource,
    paymentMethodTotals: List<PaymentMethodTotals>,
    paymentMethodNamesById: Map<UUID, String>
  ) = PaymentTypeSummaryDto(
    type = paymentSource.type,
    balanceEffect = paymentSource.balanceEffect,
    activeTotal = paymentMethodTotals.sumOf { it.activeTotal },
    voidedTotal = paymentMethodTotals.sumOf { it.voidedTotal },
    activeCount = paymentMethodTotals.sumOf { it.activeCount },
    voidedCount = paymentMethodTotals.sumOf { it.voidedCount },
    methods = paymentMethodTotals
      .map { paymentMethodTotal ->
        PaymentMethodSummaryDto(
          paymentMethodId = paymentMethodTotal.paymentMethodId,
          paymentMethodName = paymentMethodNameOf(paymentMethodTotal.paymentMethodId, paymentMethodNamesById),
          activeTotal = paymentMethodTotal.activeTotal,
          voidedTotal = paymentMethodTotal.voidedTotal
        )
      }
      .sortedBy { it.paymentMethodName }
  )
}
