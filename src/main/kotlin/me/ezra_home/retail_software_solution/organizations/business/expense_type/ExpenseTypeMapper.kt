package me.ezra_home.retail_software_solution.organizations.business.expense_type

import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeInsertDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ExpenseTypeMapper {

    fun toDomainDto(expenseTypeEntity: ExpenseTypeEntity): ExpenseTypeDto

    fun toResponseDto(expenseTypeDto: ExpenseTypeDto): ExpenseTypeResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    fun toEntity(expenseTypeInsertDto: ExpenseTypeInsertDto): ExpenseTypeEntity
}
