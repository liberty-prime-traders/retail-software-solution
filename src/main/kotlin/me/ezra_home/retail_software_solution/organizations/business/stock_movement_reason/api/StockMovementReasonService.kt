package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonCache
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonMapper
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.StockMovementReasonValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

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
        if (existing.systemDefined) throw RtsGenericException(SYSTEM_REASON_CANNOT_BE_MODIFIED)
        validator.validateUpdate(updateDto)
        return mapper.toResponseDto(cache.save(updateDto.applyTo(existing)))
    }

    fun delete(id: UUID) {
        val existing = cache.getAll().find { it.id == id } ?: return
        if (existing.systemDefined) throw RtsGenericException(SYSTEM_REASON_CANNOT_BE_DELETED)
        cache.delete(id)
    }

    fun getAll(): Collection<StockMovementReasonResponseDto> =
        cache.getAll().map { mapper.toResponseDto(it) }

    fun guardReason(movementType: MovementType, reasonId: UUID?) {
        if (reasonId == null) {
            if (movementType.reasonRequired) {
                throw RtsGenericException("A reason is required for ${movementType.name} movements")
            }
            return
        }
        val reason = cache.getAll().find { it.id == reasonId }
            ?: throw RtsGenericException("Stock movement reason $reasonId does not exist")
        if (movementType !in reason.movementTypes) {
            throw RtsGenericException("Reason '${reason.name}' does not apply to ${movementType.name} movements")
        }
    }

    companion object {
        const val SYSTEM_REASON_CANNOT_BE_MODIFIED = "System-defined reasons cannot be modified"
        const val SYSTEM_REASON_CANNOT_BE_DELETED = "System-defined reasons cannot be deleted"
    }
}
