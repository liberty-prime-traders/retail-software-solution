package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.search.SaleSearchFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.search.SaleSearchMapper
import me.ezra_home.retail_software_solution.locations.business.sale.search.SaleSearchValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.util.business.mappers.NameResolution
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.paging.PageResponse
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SaleSearchService(
  private val saleSearchFetcher: SaleSearchFetcher,
  private val salePaymentFetcher: SalePaymentFetcher,
  private val nameResolution: NameResolution
) {

  fun search(pageRequest: PageRequest<SaleSearchParameters, String>): PageResponse<SaleSummary, String> {
    SaleSearchValidator.guardValidParameters(pageRequest.parameters)
    SaleSearchValidator.guardValidPageSize(pageRequest.requestedSize)
    val cursor = KeysetSearchCursor.decode(pageRequest.previousCursor)

    val rawRows = saleSearchFetcher.search(pageRequest.parameters, cursor, pageRequest.requestedSize)
    val hasMore = rawRows.size > pageRequest.requestedSize
    val pageRows = if (hasMore) rawRows.take(pageRequest.requestedSize) else rawRows

    val paidAmountBySaleId = salePaymentFetcher.calculatePaidAmounts(pageRows.map { it.id })
    val contactNamesById = nameResolution.organizationContacts(pageRows.map { it.contactId })
    val userNamesById = nameResolution.systemUsers(pageRows.mapNotNull { it.soldByUserId })
    val contents = pageRows.map {
      SaleSearchMapper.toRowDto(it, contactNamesById, userNamesById, paidAmountBySaleId[it.id] ?: BigDecimal.ZERO)
    }

    val currentCursor = pageRows.lastOrNull()
      ?.let { KeysetSearchCursor(it.createdOn, it.id).encode() }
      ?: pageRequest.previousCursor

    return PageResponse(currentCursor = currentCursor, hasMore = hasMore, contents = contents)
  }

  fun summarize(saleSearchParameters: SaleSearchParameters): SaleSearchSummaryResponseDto {
    SaleSearchValidator.guardValidParameters(saleSearchParameters)
    SaleSearchValidator.guardSummaryHasFilter(saleSearchParameters)
    val rawRowsByStatus = saleSearchFetcher.summarize(saleSearchParameters).associateBy { it.status }

    val statuses = SaleStatus.entries.map { status ->
      rawRowsByStatus[status]?.let {
        SaleStatusSummaryDto(
          status = it.status,
          saleCount = it.saleCount,
          receivableTotal = it.receivableTotal,
          discountTotal = it.discountTotal,
          paidTotal = it.paidTotal,
          outstandingTotal = it.outstandingTotal,
          creditTotal = it.creditTotal
        )
      } ?: emptyStatusSummary(status)
    }

    val confirmed = statuses.first { it.status == SaleStatus.CONFIRMED }

    return SaleSearchSummaryResponseDto(
      statuses = statuses,
      confirmedReceivableTotal = confirmed.receivableTotal,
      confirmedDiscountTotal = confirmed.discountTotal,
      paidTotal = statuses.sumOf { it.paidTotal },
      outstandingTotal = confirmed.outstandingTotal,
      creditTotal = statuses.sumOf { it.creditTotal },
      saleCount = statuses.sumOf { it.saleCount }
    )
  }

  private fun emptyStatusSummary(status: SaleStatus): SaleStatusSummaryDto = SaleStatusSummaryDto(
    status = status,
    saleCount = 0,
    receivableTotal = BigDecimal.ZERO,
    discountTotal = BigDecimal.ZERO,
    paidTotal = BigDecimal.ZERO,
    outstandingTotal = BigDecimal.ZERO,
    creditTotal = BigDecimal.ZERO
  )
}
