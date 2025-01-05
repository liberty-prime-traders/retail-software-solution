package me.ezra_home.retail_software_solution.model.converter

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.model.enums.LocationType

@Converter(autoApply = true)
class LocationTypeConverter: EnumConverter<LocationType>(LocationType::class.java)
