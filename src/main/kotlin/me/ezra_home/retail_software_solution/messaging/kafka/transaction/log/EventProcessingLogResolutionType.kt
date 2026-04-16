package me.ezra_home.retail_software_solution.messaging.kafka.transaction.log

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class EventProcessingLogResolutionType(override val code: String) : HasCode {
    DLT_REPLAY("RPL"),
    REISSUED("RIS")
}
