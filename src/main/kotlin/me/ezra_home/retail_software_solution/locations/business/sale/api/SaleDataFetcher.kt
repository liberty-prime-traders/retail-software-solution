package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.sale.SaleAssembler
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.math.BigDecimal
import java.util.UUID

data class SaleContext(val contactId: UUID, val payableTotal: BigDecimal, val status: SaleStatus)

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SaleDataFetcher(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleAssembler: SaleAssembler,
    private val locationProductDataFetcher: LocationProductDataFetcher,
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

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun loadDraftAtVersion(saleId: UUID, expectedVersion: Long?): SaleEntity {
        val sale = saleRepository.findById(saleId).orElseThrow {
            RtsGenericException("Sale $saleId no longer exists")
        }
        if (sale.status != SaleStatus.DRAFT) {
            throw RtsGenericException("Sale ${sale.requiredReference()} is not a draft")
        }
        val expected = expectedVersion
            ?: throw RtsGenericException("Expected version must be supplied when committing an existing sale")
        if (sale.version != expected) {
            throw ObjectOptimisticLockingFailureException(SaleEntity::class.java, saleId)
        }
        return sale
    }

    fun getSaleHeader(saleId: UUID): SaleHeaderDto {
        val sale = saleRepository.findById(saleId).orElseThrow {
            RtsGenericException("Sale $saleId not found")
        }
        return SaleHeaderDto(
            id = sale.id!!,
            version = sale.version,
            status = sale.status,
            contactId = sale.contactId,
            soldById = sale.soldById,
            dateSold = sale.dateSold,
            notes = sale.notes,
        )
    }

    fun getSaleLines(saleId: UUID): List<SaleLineDto> {
        val lines = saleLineRepository.findBySaleId(saleId)
        if (lines.isEmpty()) return emptyList()
        val productSummaries = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId })
        return lines.map { line ->
            SaleLineDto(
                id = line.id!!,
                locationProductId = line.locationProductId,
                productLabel = productSummaries[line.locationProductId]?.label ?: line.locationProductId.toString(),
                quantity = line.quantity,
                unitId = line.unitId,
                conversionFactor = line.conversionFactor,
                unitPrice = line.unitPrice,
            )
        }
    }
}

