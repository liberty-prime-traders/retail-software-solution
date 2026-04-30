package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseUpdater(
    private val purchaseRepository: PurchaseRepository
) {

    fun updatePaymentStatus(purchaseId: UUID, status: PaymentStatus) {
        val purchase = purchaseRepository.getReferenceById(purchaseId)
        purchase.paymentStatus = status
        purchaseRepository.save(purchase)
    }

    fun updateNotes(id: UUID, notes: Optional<String>?) {
        notes?.let {
            val purchase = purchaseRepository.findById(id).orElseThrow { UpdatingNonExistingRecordException() }
            purchase.notes = StringUtils.getValueOrNull(it)
            purchaseRepository.save(purchase)
        }
    }
}
