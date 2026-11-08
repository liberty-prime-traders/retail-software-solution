package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.StockReplacementLayerSpecs
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID
import org.springframework.transaction.annotation.Propagation

@Service
@TransactionalOnLocationSchema
class StockReplacementLayerRecorder(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
    private val stockEntryRepository: StockEntryRepository,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun record(stockReplacementLayerRequest: StockReplacementLayerRequest): UUID {
        val revisedStockEntry = stockEntryRepository.findById(
            stockReplacementLayerRequest.revisedStockEntryId
        ).orElseThrow {
            RtsGenericException(
                "The stock of this delivery could not be found. Please contact support."
            )
        }
        return stockIncreaseRecorder.record(
            StockReplacementLayerSpecs.of(stockReplacementLayerRequest, revisedStockEntry)
        )
    }
}
