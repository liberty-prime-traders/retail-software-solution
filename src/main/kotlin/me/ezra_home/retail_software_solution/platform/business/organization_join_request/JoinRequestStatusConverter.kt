package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class JoinRequestStatusConverter : EnumConverter<JoinRequestStatus>(JoinRequestStatus::class.java)
