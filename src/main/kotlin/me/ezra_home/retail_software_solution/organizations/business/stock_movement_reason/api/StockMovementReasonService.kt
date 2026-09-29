package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonCache
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonMapper
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class StockMovementReasonService(
    private val cache: StockMovementReasonCache,
    private val mapper: StockMovementReasonMapper,
    private val validator: StockMovementReasonValidator
) {

    fun create(insertDto: StockMovementReasonInsertDto): StockMovementReasonResponseDto {
        validator.validateInsert(insertDto)
        return mapper.toResponseDto(cache.create(insertDto))
    }

    fun update(updateDto: StockMovementReasonUpdateDto): StockMovementReasonResponseDto {
        val existing = cache.getAll().find { it.id == updateDto.id } ?: throw UpdatingNonExistingRecordException()
        if (existing.systemDefined) throw RtsGenericException("System-defined reasons cannot be modified")
        validator.validateUpdate(updateDto)
        return mapper.toResponseDto(cache.save(updateDto.applyTo(existing)))
    }

    fun getAll(): Collection<StockMovementReasonResponseDto> =
        cache.getAll().map { mapper.toResponseDto(it) }
}
