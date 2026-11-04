package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineCogsEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineCogsRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SaleLineCogsBackfillStore(
    private val saleLineRepository: SaleLineRepository,
    private val saleLineCogsRepository: SaleLineCogsRepository,
) {

    fun findLinesMissingCogsRecord(batchSize: Int): List<SaleLineCogsBackfillCandidate> =
        saleLineRepository.findLinesMissingCogsRecord(PageRequest.of(0, batchSize))

    fun insertCogsRecords(candidates: List<SaleLineCogsBackfillCandidate>, referenceNumbers: List<String>) {
        saleLineCogsRepository.saveAllAndFlush(candidates.mapIndexed { index, candidate ->
            SaleLineCogsEntity(saleLineId = candidate.saleLineId, cogsTotal = candidate.cogsTotal).also {
                it.referenceNumber = referenceNumbers[index]
                it.createdOn = candidate.dateSold
            }
        })
    }

    fun countSaleLinesWithCogsTotal(): Long = saleLineRepository.countByCogsTotalIsNotNull()

    fun countCogsRecords(): Long = saleLineCogsRepository.count()
}
