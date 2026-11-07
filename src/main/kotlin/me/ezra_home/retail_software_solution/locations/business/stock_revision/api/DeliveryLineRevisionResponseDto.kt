package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.locations.business.delivery.api.ReplacementDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.ConsumerReplayDto

data class DeliveryLineRevisionResponseDto(
    val stockRevisionReferenceNumber: String,
    val replacementDeliveryLine: ReplacementDeliveryLineDto,
    val consumerReplays: List<ConsumerReplayDto>,
)
