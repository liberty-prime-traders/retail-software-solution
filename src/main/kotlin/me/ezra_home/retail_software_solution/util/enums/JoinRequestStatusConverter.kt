package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class JoinRequestStatusConverter : EnumConverter<JoinRequestStatus>(JoinRequestStatus::class.java)
