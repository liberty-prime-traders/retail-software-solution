package me.ezra_home.retail_software_solution.locations.business.purchase.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class PurchaseDeliveryLineStatusConverter : EnumConverter<PurchaseDeliveryLineStatus>(PurchaseDeliveryLineStatus::class.java)
