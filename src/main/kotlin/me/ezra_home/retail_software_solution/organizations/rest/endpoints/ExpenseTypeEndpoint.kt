package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeResponseDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeUpdateDto
import me.ezra_home.retail_software_solution.platform.business.feature.api.Feature
import me.ezra_home.retail_software_solution.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/expense-types")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ExpenseTypeEndpoint(
    private val expenseTypeService: ExpenseTypeService
) {

    @GetMapping
    fun getAll(@RequestParam(required = false) sourceType: ExpenseSourceType?): List<ExpenseTypeResponseDto> =
        expenseTypeService.getAll(sourceType)

    @PostMapping
    fun create(@RequestBody expenseTypeInsertDto: ExpenseTypeInsertDto): ExpenseTypeResponseDto =
        expenseTypeService.create(expenseTypeInsertDto)

    @PutMapping
    fun update(@RequestBody expenseTypeUpdateDto: ExpenseTypeUpdateDto): ExpenseTypeResponseDto =
        expenseTypeService.update(expenseTypeUpdateDto)
}
