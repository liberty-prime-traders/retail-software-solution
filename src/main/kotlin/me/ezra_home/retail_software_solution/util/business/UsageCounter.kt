package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.model.BaseEntity
import java.util.UUID

interface UsageCounter<T> where T: BaseEntity {
    fun incrementUsageCount(id: UUID?)
    fun decrementUsageCount(id: UUID?)

    fun incrementUsageCount(entity: T) {
        incrementUsageCount(entity.id)
    }

    fun decrementUsageCount(entity: T) {
        decrementUsageCount(entity.id)
    }

}
