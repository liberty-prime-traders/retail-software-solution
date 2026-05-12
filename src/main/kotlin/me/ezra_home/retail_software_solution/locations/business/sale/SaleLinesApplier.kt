package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleLinesApplier(
    private val saleValidator: SaleValidator,
    private val saleStockReserver: SaleStockReserver,
    private val saleLineRepository: SaleLineRepository,
) {

    fun apply(saleId: UUID, prepared: PreparedLineUpdate) {
        val resolvedBaseQuantitiesPerProduct = (prepared.newLines + prepared.updatedLines)
            .associate { it.locationProductId to it.baseQty() }
        saleValidator.guardStockForDraftUpdates(saleId, resolvedBaseQuantitiesPerProduct, prepared.productSummaries)
        saleLineRepository.saveAll(prepared.updatedLines + prepared.newLines)
        saleStockReserver.syncUpdatedReservations(prepared.updatedLines, prepared.newLines, saleId)
    }
}
