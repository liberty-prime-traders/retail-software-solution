package me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class PurchaseDeliveryStatusConverter : EnumConverter<PurchaseDeliveryStatus>(PurchaseDeliveryStatus::class.java)
