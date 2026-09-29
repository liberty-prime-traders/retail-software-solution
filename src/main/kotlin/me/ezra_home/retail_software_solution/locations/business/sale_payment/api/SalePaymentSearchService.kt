package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchCursor
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchMapper
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.SalePaymentSearchValidator
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentSearchService(
  private val salePaymentSearchFetcher: SalePaymentSearchFetcher,
  private val paymentMethodService: PaymentMethodService,
  private val contactService: ContactService
) {

  fun search(pageRequest: PageRequest<SalePaymentSearchParameters, String>): PageResponse<SalePaymentSearchResultDto, String> {
    SalePaymentSearchValidator.guardValidParameters(pageRequest.parameters)
    SalePaymentSearchValidator.guardValidPageSize(pageRequest.requestedSize)
    val cursor = SalePaymentSearchCursor.decode(pageRequest.previousCursor)

    val rawRows = salePaymentSearchFetcher.search(pageRequest.parameters, cursor, pageRequest.requestedSize)
    val hasMore = rawRows.size > pageRequest.requestedSize
    val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

    val paymentMethodNamesById = paymentMethodService.getNamesById()
    val contactNamesById = resolveContactNames(pageRows.map { it.contactId })
    val contents = pageRows.map { SalePaymentSearchMapper.toRowDto(it, paymentMethodNamesById, contactNamesById) }

    val currentCursor = pageRows.lastOrNull()
      ?.let { SalePaymentSearchCursor(it.createdOn, it.id).encode() }
      ?: pageRequest.previousCursor

    return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
  }

  fun summarize(salePaymentSearchParameters: SalePaymentSearchParameters): SalePaymentSummaryResponseDto {
    SalePaymentSearchValidator.guardValidParameters(salePaymentSearchParameters)
    SalePaymentSearchValidator.guardSummaryHasFilter(salePaymentSearchParameters)
    val rawSummaries = salePaymentSearchFetcher.summarize(salePaymentSearchParameters)
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

  private fun resolveContactNames(contactIds: List<UUID>): Map<UUID, String> {
    if (contactIds.isEmpty()) return emptyMap()
    val idSet = contactIds.toSet()
    return contactService.getAllContactDtos()
      .filter { it.id in idSet }
      .associate { it.id to it.identity.displayName }
  }
}
