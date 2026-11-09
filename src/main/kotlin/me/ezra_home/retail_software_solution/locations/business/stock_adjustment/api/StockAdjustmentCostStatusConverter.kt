package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class StockAdjustmentCostStatusConverter : EnumConverter<StockAdjustmentCostStatus>(StockAdjustmentCostStatus::class.java)
