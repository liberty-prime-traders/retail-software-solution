package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = false)
class TableNameConverter : EnumConverter<TableName>(TableName::class.java)
