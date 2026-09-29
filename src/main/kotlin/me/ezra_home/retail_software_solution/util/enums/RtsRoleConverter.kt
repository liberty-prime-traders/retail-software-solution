package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class RtsRoleConverter : EnumConverter<RtsRole>(RtsRole::class.java)
