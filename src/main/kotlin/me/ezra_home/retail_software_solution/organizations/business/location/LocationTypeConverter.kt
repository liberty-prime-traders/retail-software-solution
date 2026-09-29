package me.ezra_home.retail_software_solution.organizations.business.location

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationType
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class LocationTypeConverter : EnumConverter<LocationType>(LocationType::class.java)
