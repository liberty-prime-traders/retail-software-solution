package me.ezra_home.retail_software_solution.locations.business.catalog_sync.strategy

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.SyncMode
import org.springframework.stereotype.Component


@Component
internal class SyncStrategyRegistry(strategies: List<SyncStrategy>) {

  private val strategyMap: Map<SyncMode, SyncStrategy> = strategies.associateBy { it.getSyncMode() }

  fun getStrategy(syncMode: SyncMode): SyncStrategy {
    return strategyMap[syncMode]
      ?: throw IllegalArgumentException("No strategy registered for sync mode: $syncMode")
  }
}
