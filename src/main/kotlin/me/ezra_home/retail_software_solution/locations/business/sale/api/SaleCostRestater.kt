package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineCogsEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineCogsRepository
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation

// Appends cost rows; a sale's revenue, lines and status are never edited.
@Service
@TransactionalOnLocationSchema
class SaleCostRestater(
    private val saleLineRepository: SaleLineRepository,
    private val saleLineCogsRepository: SaleLineCogsRepository,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun restate(saleLineCostChanges: List<SaleLineCostChange>) {
        if (saleLineCostChanges.isEmpty()) return
        val saleLineByReferenceNumber = saleLineRepository
            .findByReferenceNumberIn(saleLineCostChanges.map { it.saleLineReferenceNumber })
            .associateBy { it.requiredReference() }
        val activeCogsBySaleLineId = saleLineCogsRepository
            .findBySaleLineIdInAndStatus(saleLineByReferenceNumber.values.map { it.id!! }, SaleLineCogsStatus.ACTIVE)
            .associateBy { it.saleLineId }

        saleLineCostChanges.forEach { saleLineCostChange ->
            val saleLine = saleLineByReferenceNumber[saleLineCostChange.saleLineReferenceNumber]
                ?: throw RtsGenericException("Sale line ${saleLineCostChange.saleLineReferenceNumber} not found")
            val activeCogs = activeCogsBySaleLineId[saleLine.id!!]
                ?: throw RtsGenericException("Sale line ${saleLineCostChange.saleLineReferenceNumber} has no cost record")
            val restatedCogsTotal = activeCogs.cogsTotal - saleLineCostChange.oldCost + saleLineCostChange.newCost
            if (restatedCogsTotal.signum() < 0) {
                throw RtsGenericException(
                    "Sale line ${saleLineCostChange.saleLineReferenceNumber} would have a negative cost after the revision"
                )
            }
            activeCogs.status = SaleLineCogsStatus.REPLACED
            saleLineCogsRepository.save(activeCogs)
            saleLineCogsRepository.save(SaleLineCogsEntity(saleLineId = saleLine.id!!, cogsTotal = restatedCogsTotal))
        }
    }
}
