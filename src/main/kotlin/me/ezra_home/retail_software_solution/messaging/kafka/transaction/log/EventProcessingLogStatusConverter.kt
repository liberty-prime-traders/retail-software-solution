package me.ezra_home.retail_software_solution.messaging.kafka.transaction.log

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class EventProcessingLogStatusConverter : EnumConverter<EventProcessingLogStatus>(EventProcessingLogStatus::class.java)
