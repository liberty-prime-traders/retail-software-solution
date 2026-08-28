package me.ezra_home.retail_software_solution.locations.business.purchase.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class PaymentStatusConverter : EnumConverter<PaymentStatus>(PaymentStatus::class.java)
