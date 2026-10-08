package me.ezra_home.retail_software_solution.organizations.business.expense_type.api

import me.ezra_home.retail_software_solution.organizations.business.expense_type.ExpenseTypeMapper
import me.ezra_home.retail_software_solution.organizations.business.expense_type.ExpenseTypeRepository
import me.ezra_home.retail_software_solution.organizations.business.expense_type.ExpenseTypeValidator
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ExpenseTypeService(
    private val expenseTypeRepository: ExpenseTypeRepository,
    private val expenseTypeMapper: ExpenseTypeMapper,
    private val expenseTypeValidator: ExpenseTypeValidator,
    private val accountService: AccountService
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
        expenseTypeValidator.guardInsertable(expenseTypeInsertDto)
        val savedExpenseTypeEntity = expenseTypeRepository.save(expenseTypeMapper.toEntity(expenseTypeInsertDto))
        return expenseTypeMapper.toResponseDto(expenseTypeMapper.toDomainDto(savedExpenseTypeEntity))
    }

    fun update(expenseTypeUpdateDto: ExpenseTypeUpdateDto): ExpenseTypeResponseDto {
        val expenseTypeEntity = expenseTypeRepository.findById(expenseTypeUpdateDto.id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        val existingExpenseTypeDto = expenseTypeMapper.toDomainDto(expenseTypeEntity)
        if (existingExpenseTypeDto.systemDefined && expenseTypeUpdateDto.changesAnythingButName()) {
            throw RtsGenericException("System-defined expense types can only be renamed")
        }
        val updatedExpenseTypeDto = expenseTypeUpdateDto.applyTo(existingExpenseTypeDto)
        expenseTypeValidator.guardNameAvailable(updatedExpenseTypeDto.name, updatedExpenseTypeDto.id)
        expenseTypeValidator.guardEligibilityNotEmpty(
            updatedExpenseTypeDto.eligiblePayeeTypes.size,
            updatedExpenseTypeDto.eligibleSourceTypes.size
        )
        if (updatedExpenseTypeDto.expenseAccountCode != existingExpenseTypeDto.expenseAccountCode) {
            accountService.requireActiveExpenseLeafAccount(updatedExpenseTypeDto.expenseAccountCode)
        }
        expenseTypeEntity.name = updatedExpenseTypeDto.name
        expenseTypeEntity.expenseAccountCode = updatedExpenseTypeDto.expenseAccountCode
        expenseTypeEntity.eligiblePayeeTypes = updatedExpenseTypeDto.eligiblePayeeTypes
        expenseTypeEntity.eligibleSourceTypes = updatedExpenseTypeDto.eligibleSourceTypes
        expenseTypeRepository.save(expenseTypeEntity)
        return expenseTypeMapper.toResponseDto(updatedExpenseTypeDto)
    }
}
