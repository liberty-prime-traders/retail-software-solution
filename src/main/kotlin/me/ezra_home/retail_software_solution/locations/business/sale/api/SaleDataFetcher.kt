package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class SaleContext(val contactId: UUID, val saleTotal: BigDecimal, val status: SaleStatus)

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SaleDataFetcher(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleAssembler: SaleAssembler
) {

    fun fetchTopN(n: Int?): List<SaleResponseDto> {
        val recordCount = n ?: 10
        if (recordCount <= 0) return emptyList()
        if (recordCount > 1000) throw RtsGenericException("Limit exceeds maximum of 1000")
        val sort = Sort.by(Sort.Order.asc("createdOn"))
        return saleAssembler.buildResponses(
            saleRepository.findTopN(PageRequest.of(0, recordCount, sort))
        )
    }

    fun getSaleContext(saleId: UUID): SaleContext {
        val sale = saleRepository.getReferenceById(saleId)
        val saleTotal = saleLineRepository.sumSaleTotal(saleId)
        return SaleContext(sale.contactId, saleTotal, sale.status)
    }

    fun getSaleContactId(saleId: UUID): UUID {
        return saleRepository.getReferenceById(saleId).contactId
    }
}
