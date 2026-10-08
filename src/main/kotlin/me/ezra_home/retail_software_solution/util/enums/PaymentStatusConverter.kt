package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class PaymentStatusConverter : EnumConverter<PaymentStatus>(PaymentStatus::class.java)
