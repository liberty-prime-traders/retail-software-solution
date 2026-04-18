package me.ezra_home.retail_software_solution.locations.business.kafka_log

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class EventProcessingLogStatus(override val code: String) : HasCode {
    PENDING("PND"),
    PROCESSED("PRC"),
    FAILED("FAI"),
    RETRYING("RTR"),
    PUBLISH_FAILED("PBF")
}
