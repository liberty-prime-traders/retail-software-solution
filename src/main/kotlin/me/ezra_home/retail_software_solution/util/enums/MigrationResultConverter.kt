package me.ezra_home.retail_software_solution.util.enums

import jakarta.persistence.Converter

@Converter(autoApply = true)
class MigrationResultConverter : EnumConverter<MigrationResult>(MigrationResult::class.java)
