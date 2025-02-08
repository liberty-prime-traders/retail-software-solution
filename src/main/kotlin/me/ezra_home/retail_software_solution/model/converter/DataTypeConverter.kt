package me.ezra_home.retail_software_solution.model.converter

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.model.enums.DataType

@Converter(autoApply = true)
class DataTypeConverter: EnumConverter<DataType>(DataType::class.java)