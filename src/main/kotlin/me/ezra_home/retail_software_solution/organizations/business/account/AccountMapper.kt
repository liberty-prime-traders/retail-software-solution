package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountInsertDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping


@Mapper(config = RtsMapperConfig::class)
interface AccountMapper {

    fun toDomainDto(entity: AccountEntity): AccountDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "currentBalance", constant = "0.0")
    @Mapping(target = "balanceUpdatedAt", ignore = true)
    @Mapping(target = "currencyCode", defaultValue = "KES")
    @Mapping(target = "accountIsActive", constant = "true")
    fun toEntity(insertDto: AccountInsertDto): AccountEntity
}
