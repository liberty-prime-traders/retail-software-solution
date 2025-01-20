package me.ezra_home.retail_software_solution.business.util.usagecount

import java.util.Optional
import java.util.UUID

interface UsageCountUpdater<ENTITY> where ENTITY: HasUsageCount {
    fun save(entity: ENTITY)

    fun findById(uuid: UUID): Optional<ENTITY>

    fun updateUsageCount(parameters: UsageCountUpdateParameters<ENTITY>): Boolean {
        findById(parameters.recordId).ifPresent {
            when (parameters.usageCountUpdateType) {
                UsageCountUpdateType.INCREMENT -> it.usageCount += parameters.scale
                UsageCountUpdateType.DECREMENT -> it.usageCount -= parameters.scale
            }
            save(it)
        }
        return false
    }
}
