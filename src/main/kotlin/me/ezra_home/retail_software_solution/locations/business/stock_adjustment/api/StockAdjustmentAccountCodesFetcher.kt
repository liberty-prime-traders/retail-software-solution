package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentCategoryRules
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.StockAdjustmentRepository
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentAccountCodesFetcher(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
) {

    fun getByReferenceNumber(referenceNumbers: Collection<String>): Map<String, StockAdjustmentAccountCodes> =
        stockAdjustmentRepository.findByReferenceNumberIn(referenceNumbers).associate { stockAdjustment ->
            val ledgerAccounts = StockAdjustmentCategoryRules.ledgerAccounts(
                stockAdjustment.category, stockAdjustment.direction
            )
            stockAdjustment.requiredReference() to
                StockAdjustmentAccountCodes(ledgerAccounts.debit.code, ledgerAccounts.credit.code)
        }
}
