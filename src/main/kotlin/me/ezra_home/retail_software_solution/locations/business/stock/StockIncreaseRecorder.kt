package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class StockIncreaseRecorder(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockBreakdownProvider: StockBreakdownProvider,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun record(layerSpecs: List<InboundLayerSpec>): List<UUID> {
        if (layerSpecs.isEmpty()) return emptyList()
        val locationProductIds = layerSpecs.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val runningBalances = stockBalanceFetcher.getLatestBalances(locationProductIds).toMutableMap()
        val breakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(locationProductIds)

        val savedEntryIds = mutableListOf<UUID>()
        val movements = mutableListOf<StockMovementEntity>()
        for (layerSpec in layerSpecs) {
            val entry = stockEntryRepository.save(StockInboundLayers.newLayer(layerSpec))
            val newBalance = ExactQuantity.ofStorage(runningBalances.getValue(layerSpec.locationProductId)) + layerSpec.storageQuantity
            runningBalances[layerSpec.locationProductId] = newBalance.toStorageQuantity()
            movements.add(
                StockInboundLayers.inboundMovement(
                    layerSpec, entry, newBalance,
                    breakdownByLocationProductId.getValue(layerSpec.locationProductId)
                )
            )
            savedEntryIds.add(entry.id!!)
        }
        stockMovementRepository.saveAll(movements)
        return savedEntryIds
    }
}
