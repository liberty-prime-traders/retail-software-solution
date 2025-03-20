package me.ezra_home.retail_software_solution.business.util

import java.util.UUID

interface UsageCountManager {
    fun incrementUsageCount(id: UUID?)
    fun decrementUsageCount(id: UUID?)
}
