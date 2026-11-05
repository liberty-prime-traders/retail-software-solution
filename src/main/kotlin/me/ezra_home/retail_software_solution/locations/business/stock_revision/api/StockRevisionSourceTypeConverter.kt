package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class StockRevisionSourceTypeConverter : EnumConverter<StockRevisionSourceType>(StockRevisionSourceType::class.java)
