package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleLinesUpdateApplier(
    private val saleValidator: SaleValidator,
    private val saleLineRepository: SaleLineRepository,
) {

    fun apply(saleId: UUID, updateContext: SaleLinesUpdateContext) {
        val resolvedBaseQuantitiesPerProduct = (updateContext.newLines + updateContext.updatedLines)
            .associate { it.locationProductId to it.baseQty() }
        saleValidator.guardStockForDraftUpdates(saleId, resolvedBaseQuantitiesPerProduct, updateContext.productSummaries)
        saleLineRepository.saveAll(updateContext.updatedLines + updateContext.newLines)
    }
}
