package me.ezra_home.retail_software_solution.platform.business.auth

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class PendingLinkStatusConverter : EnumConverter<PendingLinkStatus>(PendingLinkStatus::class.java)
