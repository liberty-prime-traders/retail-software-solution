package me.ezra_home.retail_software_solution.business.util.usagecount

import java.util.UUID

data class UsageCountUpdateParameters<ENTITY>  (
    val recordId: UUID,
    val scale: Long,
    val usageCountUpdateType: UsageCountUpdateType
) where ENTITY: HasUsageCount
