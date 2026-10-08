package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchMapper
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchValidator
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.mappers.NameResolution
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentSearchService(
  private val salePaymentSearchFetcher: SalePaymentSearchFetcher,
  private val paymentMethodService: PaymentMethodService,
  private val nameResolution: NameResolution
) {

  fun search(pageRequest: PageRequest<SalePaymentSearchParameters, String>): PageResponse<SalePaymentSearchResultDto, String> {
    val salePaymentSearchParameters = pageRequest.parameters.sanitized()
    SalePaymentSearchValidator.guardValidParameters(salePaymentSearchParameters)
    SalePaymentSearchValidator.guardValidPageSize(pageRequest.requestedSize)
    val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

    val rawRows = salePaymentSearchFetcher.search(salePaymentSearchParameters, cursor, pageRequest.requestedSize)
    val hasMore = rawRows.size > pageRequest.requestedSize
    val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

    val paymentMethodNamesById = paymentMethodService.getNamesById()
    val contactNamesById = nameResolution.organizationContacts(pageRows.map { it.contactId })
    val contents = pageRows.map { SalePaymentSearchMapper.toRowDto(it, paymentMethodNamesById, contactNamesById) }

    val currentCursor = pageRows.lastOrNull()
      ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
      ?: pageRequest.previousCursor

    return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
  }

  fun summarize(salePaymentSearchParameters: SalePaymentSearchParameters): SalePaymentSummaryResponseDto {
    val sanitizedSalePaymentSearchParameters = salePaymentSearchParameters.sanitized()
    SalePaymentSearchValidator.guardValidParameters(sanitizedSalePaymentSearchParameters)
    val rawSummaries = salePaymentSearchFetcher.summarize(sanitizedSalePaymentSearchParameters)
    val paymentMethodNamesById = paymentMethodService.getNamesById()

    val methods = rawSummaries.map { raw ->
      SalePaymentMethodSummaryDto(
        paymentMethodId = raw.paymentMethodId,
        paymentMethodName = paymentMethodNamesById[raw.paymentMethodId] ?: raw.paymentMethodId.toString(),
        activeTotal = raw.activeTotal,
        voidedTotal = raw.voidedTotal
      )
    }

    return SalePaymentSummaryResponseDto(
      methods = methods,
      grandActiveTotal = rawSummaries.sumOf { it.activeTotal },
      grandVoidedTotal = rawSummaries.sumOf { it.voidedTotal },
      activeCount = rawSummaries.sumOf { it.activeCount },
      voidedCount = rawSummaries.sumOf { it.voidedCount }
    )
  }
}
