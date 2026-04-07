package me.ezra_home.retail_software_solution.organizations.business.stock_item_source

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class StockItemSourceConverter : EnumConverter<StockItemSource>(StockItemSource::class.java)
