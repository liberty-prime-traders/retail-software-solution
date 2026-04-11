package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountInsertDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping


@Mapper(config = RtsMapperConfig::class, uses = [AccountMappingQualifier::class])
interface AccountMapper {

    fun toDomainDto(entity: AccountEntity): AccountDto
    fun toEntity(dto: AccountDto): AccountEntity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "balanceUpdatedAt", ignore = true)
    @Mapping(target = "accountIsActive", defaultValue = "true")
    fun toEntity(insertDto: AccountInsertDto): AccountEntity

    @Mapping(target = "parentAccount", expression = "java(parentAccount)")
    @Mapping(target = "accountIsExtensible", source = ".", qualifiedBy = [IsExtensible::class])
    fun toResponseDto(dto: AccountDto, @Context parentAccount: String? = null): AccountResponseDto

}
