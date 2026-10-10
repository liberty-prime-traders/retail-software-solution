package me.ezra_home.retail_software_solution.organizations.business.expense_type.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.expense_type.ExpenseTypeMapper
import me.ezra_home.retail_software_solution.organizations.business.expense_type.ExpenseTypeRepository
import me.ezra_home.retail_software_solution.organizations.business.expense_type.ExpenseTypeValidator
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ExpenseTypeService(
    private val expenseTypeRepository: ExpenseTypeRepository,
    private val expenseTypeMapper: ExpenseTypeMapper,
    private val expenseTypeValidator: ExpenseTypeValidator
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getById(id: UUID): ExpenseTypeDto {
        val expenseTypeEntity = expenseTypeRepository.findById(id)
            .orElseThrow { RtsGenericException("Expense type not found") }
        return expenseTypeMapper.toDomainDto(expenseTypeEntity)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getBySystemExpenseType(systemExpenseType: SystemExpenseType): ExpenseTypeDto {
        val expenseTypeEntity = expenseTypeRepository.findByCode(systemExpenseType.code)
            ?: throw RtsGenericException("System expense type ${systemExpenseType.displayName} has not been seeded")
        return expenseTypeMapper.toDomainDto(expenseTypeEntity)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(sourceType: ExpenseSourceType?): List<ExpenseTypeResponseDto> =
        expenseTypeRepository.findAll()
            .map { expenseTypeMapper.toDomainDto(it) }
            .filter { sourceType == null || sourceType in it.eligibleSourceTypes }
            .sortedBy { it.name }
            .map { expenseTypeMapper.toResponseDto(it) }

    fun create(expenseTypeInsertDto: ExpenseTypeInsertDto): ExpenseTypeResponseDto {
        val trimmedExpenseTypeInsertDto = expenseTypeInsertDto.copy(name = StringUtils.getValueOrException(expenseTypeInsertDto.name, EXPENSE_TYPE_NAME_REQUIRED))
        expenseTypeValidator.guardInsertable(trimmedExpenseTypeInsertDto)
        val savedExpenseTypeEntity = expenseTypeRepository.save(expenseTypeMapper.toEntity(trimmedExpenseTypeInsertDto))
        return expenseTypeMapper.toResponseDto(expenseTypeMapper.toDomainDto(savedExpenseTypeEntity))
    }

    fun update(expenseTypeUpdateDto: ExpenseTypeUpdateDto): ExpenseTypeResponseDto {
        val trimmedExpenseTypeUpdateDto = expenseTypeUpdateDto.copy(
            name = expenseTypeUpdateDto.name?.map { StringUtils.getValueOrException(it, EXPENSE_TYPE_NAME_REQUIRED) }
        )
        val expenseTypeEntity = expenseTypeRepository.findById(trimmedExpenseTypeUpdateDto.id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        val existingExpenseTypeDto = expenseTypeMapper.toDomainDto(expenseTypeEntity)
        if (existingExpenseTypeDto.systemDefined) {
            throw RtsGenericException("System-defined expense types can only be renamed; use the rename endpoint")
        }
        val updatedExpenseTypeDto = trimmedExpenseTypeUpdateDto.applyTo(existingExpenseTypeDto)
        expenseTypeValidator.guardNameAvailable(updatedExpenseTypeDto.name, updatedExpenseTypeDto.id)
        expenseTypeValidator.guardEligibilityNotEmpty(
            updatedExpenseTypeDto.eligiblePayeeTypes,
            updatedExpenseTypeDto.eligibleSourceTypes
        )
        if (updatedExpenseTypeDto.expenseAccountCode != existingExpenseTypeDto.expenseAccountCode) {
            expenseTypeValidator.guardAccountSelectable(updatedExpenseTypeDto.expenseAccountCode)
        }
        expenseTypeEntity.name = updatedExpenseTypeDto.name
        expenseTypeEntity.expenseAccountCode = updatedExpenseTypeDto.expenseAccountCode
        expenseTypeEntity.eligiblePayeeTypes = updatedExpenseTypeDto.eligiblePayeeTypes
        expenseTypeEntity.eligibleSourceTypes = updatedExpenseTypeDto.eligibleSourceTypes
        expenseTypeRepository.save(expenseTypeEntity)
        return expenseTypeMapper.toResponseDto(updatedExpenseTypeDto)
    }

    fun rename(expenseTypeRenameDto: ExpenseTypeRenameDto): ExpenseTypeResponseDto {
        val trimmedName = StringUtils.getValueOrException(expenseTypeRenameDto.name, EXPENSE_TYPE_NAME_REQUIRED)
        val expenseTypeEntity = expenseTypeRepository.findById(expenseTypeRenameDto.id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        expenseTypeValidator.guardNameAvailable(trimmedName, expenseTypeEntity.id)
        expenseTypeEntity.name = trimmedName
        expenseTypeRepository.save(expenseTypeEntity)
        return expenseTypeMapper.toResponseDto(expenseTypeMapper.toDomainDto(expenseTypeEntity))
    }

    private companion object {
        const val EXPENSE_TYPE_NAME_REQUIRED = "Expense type name is required"
    }
}
