package me.ezra_home.retail_software_solution.platform.business.db_migration

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class MigrationTypeConverter : EnumConverter<MigrationType>(MigrationType::class.java)
