package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCategoryDescriptor
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCreateDto
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/stock-adjustments")
class StockAdjustmentEndpoint(
    private val stockAdjustmentService: StockAdjustmentService
) {

    @PostMapping
    fun adjust(@RequestBody stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentResponseDto =
        stockAdjustmentService.adjust(stockAdjustmentCreateDto)

    @GetMapping("categories")
    fun getCategoryDescriptors(): List<StockAdjustmentCategoryDescriptor> =
        stockAdjustmentService.getCategoryDescriptors()

    @GetMapping
    fun getAll(): List<StockAdjustmentResponseDto> =
        stockAdjustmentService.getAll()
}
