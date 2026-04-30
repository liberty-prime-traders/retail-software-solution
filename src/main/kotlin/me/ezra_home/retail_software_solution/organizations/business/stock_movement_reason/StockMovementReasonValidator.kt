package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonInsertDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonUpdateDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class StockMovementReasonValidator(private val cache: StockMovementReasonCache) {

    fun validateInsert(dto: StockMovementReasonInsertDto) {
        val name = StringUtils.getValueOrException(dto.name, "Name cannot be null or empty")
        cache.getAll()
            .find { StringUtils.isEquivalent(it.name, name)}
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    fun validateUpdate(dto: StockMovementReasonUpdateDto) {
        if (dto.name == null) return
        val name = StringUtils.getValueOrException(dto.name, "Name cannot be null or empty")
        cache.getAll()
            .find { StringUtils.isEquivalent(it.name, name) && it.id != dto.id }
            ?.let { throw RtsGenericException(String.format(NAME_ALREADY_EXISTS, name)) }
    }

    companion object {
        const val NAME_ALREADY_EXISTS = "A movement reason with the name '%s' already exists"
    }
}
