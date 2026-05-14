package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.math.BigDecimal
import java.util.UUID

data class SaleContext(val contactId: UUID, val payableTotal: BigDecimal, val status: SaleStatus)

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SaleDataFetcher(
    private val saleRepository: SaleRepository,
    private val saleAssembler: SaleAssembler,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun fetchRecent(n: Int?): List<SaleResponseDto> {
        val recordCount = n ?: 10
        if (recordCount <= 0) throw RtsGenericException("Limit must be positive")
        if (recordCount > 1000) throw RtsGenericException("Limit exceeds maximum of 1000")
        val sort = Sort.by(Sort.Order.desc("createdOn"))
        return saleAssembler.buildResponses(
            saleRepository.findAll(PageRequest.of(0, recordCount, sort)).content
        )
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun lockAndGetSaleContext(saleId: UUID): SaleContext {
        val sale = lockAndGetSale(saleId)
        return SaleContext(sale.contactId, sale.payableTotal(), sale.status)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun lockAndGetSale(saleId: UUID): SaleEntity {
        entityAdvisoryLock.acquire(LockNamespaces.SALE, saleId)
        return saleRepository.getReferenceById(saleId)
    }

    fun getSaleContactId(saleId: UUID): UUID {
        return saleRepository.getReferenceById(saleId).contactId
    }
}
