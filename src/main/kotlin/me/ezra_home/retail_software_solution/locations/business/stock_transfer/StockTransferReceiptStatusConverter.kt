package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class StockTransferReceiptStatusConverter : EnumConverter<StockTransferReceiptStatus>(StockTransferReceiptStatus::class.java)
