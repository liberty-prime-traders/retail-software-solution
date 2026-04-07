package me.ezra_home.retail_software_solution.locations.business.purchase

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class PurchaseStatusConverter : EnumConverter<PurchaseStatus>(PurchaseStatus::class.java)
