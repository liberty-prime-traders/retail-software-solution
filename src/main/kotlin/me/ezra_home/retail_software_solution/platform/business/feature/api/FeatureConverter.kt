package me.ezra_home.retail_software_solution.platform.business.feature.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class FeatureConverter : EnumConverter<Feature>(Feature::class.java)
