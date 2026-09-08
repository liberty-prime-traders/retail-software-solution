package me.ezra_home.retail_software_solution.organizations.business.kafka_log

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class EventProcessingLogResolutionType(override val code: String) : HasCode {
    DLT_REPLAY("RPL"),
    REISSUED("RIS"),
    RACE_LOST("RCL")
}
