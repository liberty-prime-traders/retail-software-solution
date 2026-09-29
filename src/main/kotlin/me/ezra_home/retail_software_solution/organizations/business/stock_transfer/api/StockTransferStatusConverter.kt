package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class StockTransferStatusConverter : EnumConverter<StockTransferStatus>(StockTransferStatus::class.java)
