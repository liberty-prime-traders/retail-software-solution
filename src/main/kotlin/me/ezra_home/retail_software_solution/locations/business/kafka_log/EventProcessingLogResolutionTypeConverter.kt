package me.ezra_home.retail_software_solution.locations.business.kafka_log

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class EventProcessingLogResolutionTypeConverter : EnumConverter<EventProcessingLogResolutionType>(EventProcessingLogResolutionType::class.java)
